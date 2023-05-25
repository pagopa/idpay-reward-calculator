package it.gov.pagopa.common.kafka;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.common.reactive.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.utils.MemoryAppender;
import it.gov.pagopa.common.utils.TestUtils;
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
import org.junit.jupiter.api.Assertions;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.util.Pair;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Utilities when performing Kafka tests */
@ConditionalOnClass(EmbeddedKafkaBroker.class)
@Service
public class KafkaTestUtilitiesService {

    public static final String GROUPID_TEST_CHECK = "idpay-group-test-check";
    @Autowired
    private EmbeddedKafkaBroker kafkaBroker;
    @Autowired
    private KafkaTemplate<byte[], byte[]> template;

    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.cloud.stream.kafka.binder.zkNodes}")
    private String zkNodes;

    @Autowired
    private ObjectMapper objectMapper;

    /** It will return usefull URLs related to embedded kafka */
    public String getKafkaUrls() {
        return "bootstrapServers: %s, zkNodes: %s".formatted(bootstrapServers, zkNodes);
    }

//region consume messages
    /** It will configure and return a consumer attached to embedded kafka */
    public Consumer<String, String> getEmbeddedKafkaConsumer(String topic, String groupId) {
        return getEmbeddedKafkaConsumer(topic, groupId, true);
    }

    /** It will configure and return a consumer */
    public Consumer<String, String> getEmbeddedKafkaConsumer(String topic, String groupId, boolean attachToBroker) {
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

    /** It will read messages from the beginning of the provided topic, waiting until the expectedMessagesCount has reached (it could read more than it) */
    public void readFromEmbeddedKafka(String topic, java.util.function.Consumer<ConsumerRecord<String, String>> consumeMessage, Integer expectedMessagesCount, Duration timeout) {
        readFromEmbeddedKafka(getEmbeddedKafkaConsumer(topic, GROUPID_TEST_CHECK), consumeMessage, true, expectedMessagesCount, timeout);
    }

    /** It will read messages from the provided consumer, waiting until the expectedMessagesCount has reached (it could read more than it), calling the consumer for each of them */
    public void readFromEmbeddedKafka(Consumer<String, String> consumer, java.util.function.Consumer<ConsumerRecord<String, String>> consumeMessage, boolean consumeFromBeginning, Integer expectedMessagesCount, Duration timeout) {
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

    /** It will reset consumer offset to the beginning */
    public void consumeFromBeginning(Consumer<String, String> consumer) {
        consumer.seekToBeginning(consumer.assignment());
    }

    /** It will read messages from the beginning of the provided topic, waiting until the expectedMessagesCount has reached (it could read more than it) */
    public List<ConsumerRecord<String, String>> consumeMessages(String topic, int expectedNumber, long maxWaitingMs) {
        return consumeMessages(topic, GROUPID_TEST_CHECK, expectedNumber, maxWaitingMs);
    }

    /** It will read messages of the provided topic, waiting until the expectedMessagesCount has reached (it could read more than it) */
    public List<ConsumerRecord<String, String>> consumeMessages(String topic, String groupId, int expectedNumber, long maxWaitingMs) {
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
//end region

//region publish messages
    /** It will serialize the payload before to invoke {@link #publishIntoEmbeddedKafka(String, Iterable, String, String)} */
    public void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, Object payload) {
        try {
            publishIntoEmbeddedKafka(topic, headers, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private int totalMessageSentCounter = 0;
    /** It will publish the provided headers and payload to the provided topic, adding randomly some headers that should be handled by the {@link BaseKafkaConsumer} */
    public void publishIntoEmbeddedKafka(String topic, Iterable<Header> headers, String key, String payload) {
        final RecordHeader retryHeader = new RecordHeader(KafkaConstants.ERROR_MSG_HEADER_RETRY, "1".getBytes(StandardCharsets.UTF_8));
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
//endregion

//region offset check
    /** To get commits on the provided topic performed by the provided group */
    public Map<TopicPartition, OffsetAndMetadata> getCommittedOffsets(String topic, String groupId){
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, groupId, false)) {
            return consumer.committed(consumer.partitionsFor(topic).stream().map(p-> new TopicPartition(topic, p.partition())).collect(Collectors.toSet()));
        }
    }

    /** It will invoke {@link #checkCommittedOffsets(String, String, long, int, int)} configuring a default timeout */
    public Map<TopicPartition, OffsetAndMetadata> checkCommittedOffsets(String topic, String groupId, long expectedCommittedMessages){
        return checkCommittedOffsets(topic, groupId, expectedCommittedMessages, 20, 500);
    }

    // Cannot use directly Awaitlity cause the Callable condition is performed on separate thread, which will go into conflict with the consumer Kafka access
    /** It will assert the expected messages committed on the provided topic performed by the provided group */
    public Map<TopicPartition, OffsetAndMetadata> checkCommittedOffsets(String topic, String groupId, long expectedCommittedMessages, int maxAttempts, int millisAttemptDelay){
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
                TestUtils.wait(millisAttemptDelay, TimeUnit.MILLISECONDS);
            }
        }
        throw lastException;
    }

    /** It will return the offset of the last message on the provided topic */
    public Map<TopicPartition, Long> getEndOffsets(String topic){
        try (Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topic, GROUPID_TEST_CHECK, false)) {
            return consumer.endOffsets(consumer.partitionsFor(topic).stream().map(p-> new TopicPartition(topic, p.partition())).toList());
        }
    }

    /** It will assert the offset of the last message on the provided topic */
    public Map<TopicPartition, Long> checkPublishedOffsets(String topic, long expectedPublishedMessages){
        Map<TopicPartition, Long> endOffsets = getEndOffsets(topic);
        Assertions.assertEquals(expectedPublishedMessages, endOffsets.values().stream().mapToLong(x->x).sum());
        return endOffsets;
    }
//endregion

//region check commit by logs
    protected MemoryAppender commitLogMemoryAppender;
    /** To be called before each test in order to perform the asserts on {@link #assertCommitOrder(String, int)} */
    public void setupCommitLogMemoryAppender() {
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(BaseKafkaConsumer.class.getName());
        commitLogMemoryAppender = new MemoryAppender();
        commitLogMemoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(commitLogMemoryAppender);
        commitLogMemoryAppender.start();
    }

    private final Pattern partitionCommitsPattern = Pattern.compile("partition (\\d+): (\\d+) - (\\d+)");
    /** It will assert the right offset commit and the total messages by the provided {@link BaseKafkaConsumer#getFlowName()}.<br />
     * In order to be used, you have to call {@link #setupCommitLogMemoryAppender()} before each test */
    public void assertCommitOrder(String flowName, int totalSendMessages) {
        Map<Integer, Integer> partition2last = new HashMap<>(Map.of(0, -1, 1, -1));
        for (ILoggingEvent loggedEvent : commitLogMemoryAppender.getLoggedEvents()) {
            if(loggedEvent.getMessage().equals("[KAFKA_COMMIT][{}] Committing {} messages: {}") && flowName.equals(loggedEvent.getArgumentArray()[0])){
                Arrays.stream(((String)loggedEvent.getArgumentArray()[2]).split(";"))
                        .forEach(s -> {
                            Matcher matcher = partitionCommitsPattern.matcher(s);
                            Assertions.assertTrue(matcher.matches(), "Unexpected partition commit string: " + s);
                            int partition = Integer.parseInt(matcher.group(1));
                            int startOffset = Integer.parseInt(matcher.group(2));
                            int endOffset = Integer.parseInt(matcher.group(3));
                            Assertions.assertTrue(endOffset>=startOffset, "EndOffset less than StartOffset!: " + s);

                            Integer lastCommittedOffset = partition2last.get(partition);
                            Assertions.assertEquals(lastCommittedOffset, startOffset-1);
                            partition2last.put(partition, endOffset);
                        });
            }
        }

        Assertions.assertEquals(totalSendMessages, partition2last.values().stream().mapToInt(x->x+1).sum());
    }
//endregion

//region error topic
    public void checkErrorsPublished(String topicErrors, Pattern errorUseCaseIdPatternMatch, int expectedErrorMessagesNumber, long maxWaitingMs, List<Pair<Supplier<String>, java.util.function.Consumer<ConsumerRecord<String, String>>>> errorUseCases) {
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

    public void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey, Function<String, String> normalizePayload) {
        checkErrorMessageHeaders(srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, true, true, normalizePayload);
    }
    public void checkErrorMessageHeaders(String srcTopic, String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey, boolean expectRetryHeader, boolean expectedAppNameHeader, Function<String, String> normalizePayload) {
        Assertions.assertEquals(expectedAppNameHeader? applicationName : null, TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME));
        Assertions.assertEquals(expectedAppNameHeader? group : null, TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_GROUP));

        Assertions.assertEquals("kafka", TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_SRC_TYPE));
        Assertions.assertEquals(bootstrapServers, TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_SRC_SERVER));
        Assertions.assertEquals(srcTopic, TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_SRC_TOPIC));
        Assertions.assertNotNull(errorMessage.headers().lastHeader(KafkaConstants.ERROR_MSG_HEADER_STACKTRACE));
        Assertions.assertEquals(errorDescription, TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_DESCRIPTION));
        if(expectRetryHeader){
            Assertions.assertEquals("1", TestUtils.getHeaderValue(errorMessage, KafkaConstants.ERROR_MSG_HEADER_RETRY)); // to test if headers are correctly propagated
        }
        Assertions.assertEquals(normalizePayload.apply(expectedPayload), normalizePayload.apply(errorMessage.value()));
        if(expectedKey!=null) {
            Assertions.assertEquals(expectedKey, errorMessage.key());
        }
    }
//end region

}
