package it.gov.pagopa.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.process.runtime.Executable;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.stream.StreamsHealthIndicator;
import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
        "${spring.cloud.stream.bindings.trxProcessorOut-out-0.destination}",
        "${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.errors-out-0.destination}",
        "${spring.cloud.stream.bindings.hpanInitiativeConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.hpanUpdateOutcome-out-0.destination}",
        "${spring.cloud.stream.bindings.trxProducer-out-0.destination}", // TODO remove me
}, controlledShutdown = true)
@TestPropertySource(
        properties = {
                // even if enabled into application.yml, spring test will not load it https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications.jmx
                "spring.jmx.enabled=true",

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
                "spring.cloud.stream.binders.kafka-idpay-splitter-producer.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-idpay.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-idpay-rule.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-errors.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-idpay-hpan-update.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-hpan-update-outcome.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-rtd-producer.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}", // TODO remove me
                //endregion

                //region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.org.springframework.boot.autoconfigure.mongo.embedded=WARN",
                "spring.mongodb.embedded.version=4.0.21",
                //endregion
        })
@AutoConfigureDataMongo
@AutoConfigureWebTestClient
public abstract class BaseIntegrationTest {

    @Value("${spring.application.name}")
    public String applicationName;

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected EmbeddedKafkaBroker kafkaBroker;
    @Autowired
    protected KafkaTemplate<byte[], byte[]> template;

    @Autowired(required = false)
    private MongodExecutable embeddedMongoServer;

    @Value("${spring.data.mongodb.uri}")
    private String mongodbUri;

    @Autowired
    protected DroolsRuleRepository droolsRuleRepository;

    @Autowired
    protected StreamsHealthIndicator streamsHealthIndicator;

    @Autowired
    protected ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.cloud.stream.kafka.binder.zkNodes}")
    private String zkNodes;

    @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.destination}")
    protected String topicRewardProcessorRequest;
    @Value("${spring.cloud.stream.bindings.trxProcessorOut-out-0.destination}")
    protected String topicRewardProcessorOutcome;
    @Value("${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.destination}")
    protected String topicRewardRuleConsumer;
    @Value("${spring.cloud.stream.bindings.hpanInitiativeConsumer-in-0.destination}")
    protected String topicHpanInitiativeLookupConsumer;
    @Value("${spring.cloud.stream.bindings.errors-out-0.destination}")
    protected String topicErrors;
    @Value("${spring.cloud.stream.bindings.hpanUpdateOutcome-out-0.destination}")
    protected String topicHpanUpdateOutcome;

    @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.group}")
    protected String groupIdRewardProcessorRequest;
    @Value("${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.group}")
    protected String groupIdRewardRuleConsumer;
    @Value("${spring.cloud.stream.bindings.hpanInitiativeConsumer-in-0.group}")
    protected String groupIdHpanInitiativeLookupConsumer;

    @Value("${spring.redis.url}")
    protected String redisUrl;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TimeZone.setDefault(TimeZone.getTimeZone(CommonConstants.ZONEID));

        unregisterMBean("kafka.*:*");
        unregisterMBean("org.springframework.*:*");
    }

    private static void unregisterMBean(String objectName) throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException {
        ObjectName mbeanName = new ObjectName(objectName);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        for (ObjectInstance mBean : mBeanServer.queryMBeans(mbeanName, null)) {
            mBeanServer.unregisterMBean(mBean.getObjectName());
        }
    }

    @PostConstruct
    public void logEmbeddedServerConfig() throws NoSuchFieldException, UnknownHostException {
        String mongoUrl;
        if(embeddedMongoServer != null) {
            Field mongoEmbeddedServerConfigField = Executable.class.getDeclaredField("config");
            mongoEmbeddedServerConfigField.setAccessible(true);
            MongodConfig mongodConfig = (MongodConfig) ReflectionUtils.getField(mongoEmbeddedServerConfigField, embeddedMongoServer);
            Net mongodNet = Objects.requireNonNull(mongodConfig).net();

            mongoUrl="mongodb://%s:%s".formatted(mongodNet.getServerAddress().getHostAddress(), mongodNet.getPort());
        } else {
            mongoUrl=mongodbUri.replaceFirst(":[^:]+(?=:[0-9]+)", "");
        }

        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        Embedded kafka: %s
                        Embedded redis: %s
                        ************************
                        """,
                mongoUrl,
                "bootstrapServers: %s, zkNodes: %s".formatted(bootstrapServers, zkNodes),
                redisUrl);
    }

    @Test
    void testHealthIndicator(){
        Health health = streamsHealthIndicator.health();
        Assertions.assertEquals(Status.UP, health.getStatus());
    }

    protected Consumer<String, String> getEmbeddedKafkaConsumer(String topic, String groupId) {
        return getEmbeddedKafkaConsumer(topic, groupId, true);
    }

    protected Consumer<String, String> getEmbeddedKafkaConsumer(String topic, String groupId, boolean attachToBroker) {
        if (!kafkaBroker.getTopics().contains(topic)) {
            kafkaBroker.addTopics(topic);
        }

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "true", kafkaBroker);
        consumerProps.put("key.deserializer", StringDeserializer.class);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<String, String> consumer = cf.createConsumer();
        if(attachToBroker){
            kafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);
        }
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
        return consumeMessages(topic, "idpay-group-test-check", expectedNumber, maxWaitingMs);
    }

    protected List<ConsumerRecord<String, String>> consumeMessages(String topic, String groupId, int expectedNumber, long maxWaitingMs) {
        long startTime = System.currentTimeMillis();
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, groupId)) {

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

    private int totalMessageSentCounter = 0;
    protected void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, String payload) {
        final RecordHeader retryHeader = new RecordHeader("RETRY", "1".getBytes(StandardCharsets.UTF_8));
        final RecordHeader applicationNameHeader = new RecordHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, applicationName.getBytes(StandardCharsets.UTF_8));

        AtomicBoolean containAppNameHeader = new AtomicBoolean(false);
        if(headers!= null){
            headers.forEach(h -> {
                if(h.key().equals(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME)){
                    containAppNameHeader.set(true);
                }
            });
        }

        final RecordHeader[] additionalHeaders;
        if(totalMessageSentCounter++%2 == 0 || containAppNameHeader.get()){
            additionalHeaders= new RecordHeader[]{retryHeader};
        } else {
            additionalHeaders= new RecordHeader[]{retryHeader, applicationNameHeader};
        }

        if (headers == null) {
            headers = new RecordHeaders(additionalHeaders);
        } else {
            headers = Stream.concat(
                            StreamSupport.stream(headers.spliterator(), false),
                            Arrays.stream(additionalHeaders))
                    .collect(Collectors.toList());
        }
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, null, key == null ? null : key.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8), headers);
        template.send(record);
    }

    protected Map<TopicPartition, OffsetAndMetadata> getCommittedOffsets(String topic, String groupId){
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, groupId, false)) {
            return consumer.committed(consumer.partitionsFor(topic).stream().map(p-> new TopicPartition(topic, p.partition())).collect(Collectors.toSet()));
        }
    }

    protected Map<TopicPartition, OffsetAndMetadata> checkCommittedOffsets(String topic, String groupId, long expectedCommittedMessages){
        return checkCommittedOffsets(topic, groupId, expectedCommittedMessages, 20, 500);
    }

    // Cannot use directly Awaitlity cause the Callable condition is performed on separate thread, which will go into conflict with the consumer Kafka access
    protected Map<TopicPartition, OffsetAndMetadata> checkCommittedOffsets(String topic, String groupId, long expectedCommittedMessages, int maxAttempts, int millisAttemptDelay){
        RuntimeException lastException = null;
        if(maxAttempts<=0){
            maxAttempts=1;
        }

        for(;maxAttempts>0; maxAttempts--){
            try {
                final Map<TopicPartition, OffsetAndMetadata> commits = getCommittedOffsets(topic, groupId);
                Assertions.assertEquals(expectedCommittedMessages, commits.values().stream().mapToLong(OffsetAndMetadata::offset).sum());
                return commits;
            } catch (Throwable e){
                lastException = new RuntimeException(e);
                wait(millisAttemptDelay, TimeUnit.MILLISECONDS);
            }
        }
        throw lastException;
    }

    protected Map<TopicPartition, Long> getEndOffsets(String topic){
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, "idpay-group-test-check", false)) {
            return consumer.endOffsets(consumer.partitionsFor(topic).stream().map(p-> new TopicPartition(topic, p.partition())).toList());
        }
    }

    protected Map<TopicPartition, Long> checkPublishedOffsets(String topic, long expectedPublishedMessages){
        Map<TopicPartition, Long> endOffsets = getEndOffsets(topic);
        Assertions.assertEquals(expectedPublishedMessages, endOffsets.values().stream().mapToLong(x->x).sum());
        return endOffsets;
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

    protected static void wait(long timeout, TimeUnit timeoutUnit) {
        try{
            Awaitility.await().timeout(timeout, timeoutUnit).until(()->false);
        } catch (ConditionTimeoutException ex){
            // Do Nothing
        }
    }

    protected final Pattern errorUseCaseIdPatternMatch = getErrorUseCaseIdPatternMatch();

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"initiativeId\":\"id_([0-9]+)_?[^\"]*\"");
    }

    protected void checkErrorsPublished(int expectedErrorMessagesNumber, long maxWaitingMs, List<Pair<Supplier<String>, java.util.function.Consumer<ConsumerRecord<String, String>>>> errorUseCases) {
        final List<ConsumerRecord<String, String>> errors = consumeMessages(topicErrors, expectedErrorMessagesNumber, maxWaitingMs);
        for (final ConsumerRecord<String, String> record : errors) {
            final Matcher matcher = errorUseCaseIdPatternMatch.matcher(record.value());
            int useCaseId = matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
            if (useCaseId == -1) {
                throw new IllegalStateException("UseCaseId not recognized! " + record.value());
            }
            errorUseCases.get(useCaseId).getSecond().accept(record);
        }
    }

    protected void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        checkErrorMessageHeaders(srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, true, true);
    }
    protected void checkErrorMessageHeaders(String srcTopic, String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey, boolean expectRetryHeader, boolean expectedAppNameHeader) {
        Assertions.assertEquals(expectedAppNameHeader? applicationName : null, TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME));
        Assertions.assertEquals(expectedAppNameHeader? group : null, TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_GROUP));

        Assertions.assertEquals("kafka", TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_SRC_TYPE));
        Assertions.assertEquals(bootstrapServers, TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_SRC_SERVER));
        Assertions.assertEquals(srcTopic, TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_SRC_TOPIC));
        Assertions.assertNotNull(errorMessage.headers().lastHeader(KafkaConstants.ERROR_MSG_HEADER_STACKTRACE));
        Assertions.assertEquals(errorDescription, TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_DESCRIPTION));
        if(expectRetryHeader){
            Assertions.assertEquals("1", TestUtils.getHeaderValue(errorMessage, "RETRY")); // to test if headers are correctly propagated
        }
        Assertions.assertEquals(normalizePayload(expectedPayload), normalizePayload(errorMessage.value()));
        if(expectedKey!=null) {
            Assertions.assertEquals(expectedKey, errorMessage.key());
        }
    }

    protected String normalizePayload(String expectedPayload) {
        String temp = removeDateTimeField(expectedPayload, "elaborationDateTime");
        temp = removeFieldValue(temp, "ruleEngineTopicPartition");
        temp = removeFieldValue(temp, "ruleEngineTopicOffset");
        return removeDateTimeField(temp,"timestamp");
    }

    protected String removeDateTimeField(String payload, String fieldName){
        return payload.replaceAll("(\""+fieldName+"\":\"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}):[0-9]{2}:[0-9]{2}\\.?[0-9]*\"", "$1:--\"");
    }

    protected String removeFieldValue(String payload, String fieldName) {
        return payload.replaceAll("(\""+fieldName+"\":)(?:[^,}]+)", "$1:null");
    }
}
