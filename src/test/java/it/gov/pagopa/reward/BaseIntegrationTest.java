package it.gov.pagopa.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.process.runtime.Executable;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(topics = {
        "${spring.cloud.stream.bindings.trxProcessor-in-0.destination}",
        "${spring.cloud.stream.bindings.trxProcessor-out-0.destination}",
        "${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.errors-out-0.destination}",
        "${spring.cloud.stream.bindings.hpanInitiativeConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.trxProducer-out-0.destination}", // TODO remove me
}, controlledShutdown = true)
@TestPropertySource(
        properties = {
                //region common feature disabled
                "app.reward-rule.cache.refresh-ms-rate=60000",
                "logging.level.it.gov.pagopa.reward.service.ErrorNotifierServiceImpl=WARN",
                //endregion

                //region kafka brokers
                "logging.level.org.apache.zookeeper=WARN",
                "logging.level.org.apache.kafka=WARN",
                "logging.level.kafka=WARN",
                "logging.level.state.change.logger=WARN",
                "spring.cloud.stream.kafka.binder.configuration.security.protocol=PLAINTEXT",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.kafka.binder.zkNodes=${spring.embedded.zookeeper.connect}",
                "spring.cloud.stream.binders.kafka-idpay-splitter.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-idpay.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-idpay-rule.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-errors.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-idpay-hpan-update.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-rtd-producer.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}", // TODO remove me
                //endregion

                //region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.org.springframework.boot.autoconfigure.mongo.embedded=WARN",
                "spring.mongodb.embedded.version=4.0.21",
                //endregion
        })
@AutoConfigureDataMongo
public abstract class BaseIntegrationTest {
    @Autowired
    protected EmbeddedKafkaBroker kafkaBroker;
    @Autowired
    protected KafkaTemplate<byte[], byte[]> template;

    @Autowired
    private MongodExecutable embeddedMongoServer;

    @Autowired
    protected DroolsRuleRepository droolsRuleRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.cloud.stream.kafka.binder.zkNodes}")
    private String zkNodes;


    @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.destination}")
    protected String topicRewardProcessorRequest;
    @Value("${spring.cloud.stream.bindings.trxProcessor-out-0.destination}")
    protected String topicRewardProcessorOutcome;
    @Value("${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.destination}")
    protected String topicRewardRuleConsumer;
    @Value("${spring.cloud.stream.bindings.hpanInitiativeConsumer-in-0.destination}")
    protected String topicHpanInitiativeLookupConsumer;
    @Value("${spring.cloud.stream.bindings.errors-out-0.destination}")
    protected String topicErrors;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));

        ObjectName kafkaServerMbeanName = new ObjectName("kafka.server:type=app-info,id=0");
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        if (mBeanServer.isRegistered(kafkaServerMbeanName)) {
            mBeanServer.unregisterMBean(kafkaServerMbeanName);
        }
    }

    @PostConstruct
    public void logEmbeddedServerConfig() throws NoSuchFieldException, UnknownHostException {
        Field mongoEmbeddedServerConfigField = Executable.class.getDeclaredField("config");
        mongoEmbeddedServerConfigField.setAccessible(true);
        MongodConfig mongodConfig = (MongodConfig) ReflectionUtils.getField(mongoEmbeddedServerConfigField, embeddedMongoServer);
        Net mongodNet = Objects.requireNonNull(mongodConfig).net();

        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        Embedded kafka: %s
                        ************************
                        """,
                "mongo://%s:%s".formatted(mongodNet.getServerAddress().getHostAddress(), mongodNet.getPort()),
                "bootstrapServers: %s, zkNodes: %s".formatted(bootstrapServers, zkNodes));
    }

    protected Consumer<String, String> getEmbeddedKafkaConsumer(String topic, String groupId) {
        if (!kafkaBroker.getTopics().contains(topic)) {
            kafkaBroker.addTopics(topic);
        }

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "true", kafkaBroker);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = cf.createConsumer();
        kafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);
        return consumer;
    }

    protected void readFromEmbeddedKafka(String topic, String groupId, java.util.function.Consumer<ConsumerRecord<String, String>> consumeMessage, Integer expectedMessagesCount, Duration timeout) {
        readFromEmbeddedKafka(getEmbeddedKafkaConsumer(topic, groupId), consumeMessage, true, expectedMessagesCount, timeout);
    }

    protected void readFromEmbeddedKafka(Consumer<String, String> consumer, java.util.function.Consumer<ConsumerRecord<String, String>> consumeMessage, boolean consumeFromBeginning, Integer expectedMessagesCount, Duration timeout) {
        if (consumeFromBeginning) {
            consumeFromBeginning(consumer);
        }
        int i = 0;
        while (i < expectedMessagesCount) {
            ConsumerRecords<String, String> published = consumer.poll(timeout);
            for (ConsumerRecord<String, String> stringStringConsumerRecord : published) {
                consumeMessage.accept(stringStringConsumerRecord);
                i++;
            }
        }

    }

    protected void consumeFromBeginning(Consumer<String, String> consumer) {
        consumer.seekToBeginning(consumer.assignment());
    }

    protected List<ConsumerRecord<String, String>> consumeMessages(String topic, int expectedNumber, long maxWaitingMs) {
        long startTime = System.currentTimeMillis();
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, "idpay-group")) {

            List<ConsumerRecord<String, String>> payloadConsumed = new ArrayList<>(expectedNumber);
            while (payloadConsumed.size() < expectedNumber) {
                if (System.currentTimeMillis() - startTime > maxWaitingMs) {
                    Assertions.fail("timeout of %d ms expired. Read %d messages of %d".formatted(maxWaitingMs, payloadConsumed.size(), expectedNumber));
                }
                consumer.poll(Duration.ofMillis(7000)).iterator().forEachRemaining(payloadConsumed::add);
            }
            return payloadConsumed;
        }
    }

    protected void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, Object payload) {
        try {
            publishIntoEmbeddedKafka(topic, headers, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, String payload) {
        final RecordHeader retryHeader = new RecordHeader("RETRY", "1".getBytes());
        if (headers == null) {
            headers = new RecordHeaders(new RecordHeader[]{retryHeader});
        } else {
            headers = Stream.concat(
                            StreamSupport.stream(headers.spliterator(), false),
                            Stream.of(retryHeader))
                    .collect(Collectors.toList());
        }
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, null, key == null ? null : key.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8), headers);
        template.send(record);
    }

    protected static void waitFor(Callable<Boolean> test, Supplier<String> buildTestFailureMessage, int maxAttempts, int millisAttemptDelay) {
        try {
            await()
                    .pollInterval(millisAttemptDelay, TimeUnit.MILLISECONDS)
                    .atMost((long) maxAttempts * millisAttemptDelay, TimeUnit.MILLISECONDS)
                    .until(test);
        } catch (RuntimeException e) {
            Assertions.fail(buildTestFailureMessage.get(), e);
        }
    }

    protected final Pattern errorUseCaseIdPatternMatch = getErrorUseCaseIdPatternMatch();

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"initiativeId\":\"id_([0-9]+)_?[^\"]*\"");
    }

    protected void checkErrorsPublished(int notValidRules, long maxWaitingMs, List<Pair<Supplier<String>, java.util.function.Consumer<ConsumerRecord<String, String>>>> errorUseCases) {
        final List<ConsumerRecord<String, String>> errors = consumeMessages(topicErrors, notValidRules, maxWaitingMs);
        for (final ConsumerRecord<String, String> record : errors) {
            final Matcher matcher = errorUseCaseIdPatternMatch.matcher(record.value());
            int useCaseId = matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
            if (useCaseId == -1) {
                throw new IllegalStateException("UseCaseId not recognized! " + record.value());
            }
            errorUseCases.get(useCaseId).getSecond().accept(record);
        }
    }

    protected void checkErrorMessageHeaders(String srcTopic, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        Assertions.assertEquals(bootstrapServers, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_SERVER));
        Assertions.assertEquals(srcTopic, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_SRC_TOPIC));
        Assertions.assertNotNull(errorMessage.headers().lastHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_STACKTRACE));
        Assertions.assertEquals(errorDescription, TestUtils.getHeaderValue(errorMessage, ErrorNotifierServiceImpl.ERROR_MSG_HEADER_DESCRIPTION));
        Assertions.assertEquals("1", TestUtils.getHeaderValue(errorMessage, "RETRY")); // to test if headers are correctly propagated
        Assertions.assertEquals(errorMessage.value(), expectedPayload);
    }
}
