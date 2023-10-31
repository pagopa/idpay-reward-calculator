package it.gov.pagopa.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.common.kafka.KafkaTestUtilitiesService;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.mongo.singleinstance.AutoConfigureSingleInstanceMongodb;
import it.gov.pagopa.common.stream.StreamsHealthIndicator;
import it.gov.pagopa.common.utils.TestIntegrationUtils;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.connector.repository.DroolsRuleRepository;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@SpringBootTest
@EmbeddedKafka(topics = {
        "${spring.cloud.stream.bindings.trxProcessor-in-0.destination}",
        "${spring.cloud.stream.bindings.trxProcessorOut-out-0.destination}",
        "${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.errors-out-0.destination}",
        "${spring.cloud.stream.bindings.hpanInitiativeConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.hpanUpdateOutcome-out-0.destination}",
        "${spring.cloud.stream.bindings.commandsConsumer-in-0.destination}",
        "${spring.cloud.stream.bindings.trxProducer-out-0.destination}", // TODO remove me
}, controlledShutdown = true)
@TestPropertySource(
        properties = {
                // even if enabled into application.yml, spring test will not load it https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.spring-boot-applications.jmx
                "spring.jmx.enabled=true",

                //region common feature disabled
                "app.reward-rule.cache.refresh-ms-rate=60000",
                "logging.level.it.gov.pagopa.common.kafka.service.ErrorNotifierServiceImpl=WARN",
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
                "spring.cloud.stream.binders.kafka-commands.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-rtd-producer.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}", // TODO remove me
                //endregion

                //region mongodb
                "logging.level.org.mongodb.driver=WARN",
                "logging.level.de.flapdoodle.embed.mongo.spring.autoconfigure=WARN",
                "de.flapdoodle.mongodb.embedded.version=4.2.24",
                //endregion
        })
@AutoConfigureWebTestClient
@AutoConfigureSingleInstanceMongodb
public abstract class BaseIntegrationTest {

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected KafkaTestUtilitiesService kafkaTestUtilitiesService;
    @Autowired
    protected MongoTestUtilitiesService mongoTestUtilitiesService;

    @Autowired
    protected DroolsRuleRepository droolsRuleRepository;

    @Autowired
    protected StreamsHealthIndicator streamsHealthIndicator;

    @Autowired
    protected ObjectMapper objectMapper;

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
    @Value("${spring.cloud.stream.bindings.commandsConsumer-in-0.destination}")
    protected String topicCommands;

    @Value("${spring.cloud.stream.bindings.trxProcessor-in-0.group}")
    protected String groupIdRewardProcessorRequest;
    @Value("${spring.cloud.stream.bindings.rewardRuleConsumer-in-0.group}")
    protected String groupIdRewardRuleConsumer;
    @Value("${spring.cloud.stream.bindings.hpanInitiativeConsumer-in-0.group}")
    protected String groupIdHpanInitiativeLookupConsumer;
    @Value("${spring.cloud.stream.bindings.commandsConsumer-in-0.group}")
    protected String groupIdCommandsConsumer;

    @Value("${spring.data.redis.url}")
    protected String redisUrl;

    @BeforeAll
    public static void unregisterPreviouslyKafkaServers() throws MalformedObjectNameException, MBeanRegistrationException, InstanceNotFoundException {
        TestIntegrationUtils.setDefaultTimeZoneAndUnregisterCommonMBean();
    }

    @PostConstruct
    public void logEmbeddedServerConfig() {
        System.out.printf("""
                        ************************
                        Embedded mongo: %s
                        Embedded kafka: %s
                        Embedded redis: %s
                        ************************
                        """,
                mongoTestUtilitiesService.getMongoUrl(),
                kafkaTestUtilitiesService.getKafkaUrls(),
                redisUrl);
    }

    @Test
    void testHealthIndicator(){
        Health health = streamsHealthIndicator.health();
        Assertions.assertEquals(Status.UP, health.getStatus());
    }

    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"initiativeId\":\"id_([0-9]+)_?[^\"]*\"");
    }

    protected void checkErrorsPublished(int expectedErrorMessagesNumber, long maxWaitingMs, List<Pair<Supplier<String>, java.util.function.Consumer<ConsumerRecord<String, String>>>> errorUseCases) {
        kafkaTestUtilitiesService.checkErrorsPublished(topicErrors, getErrorUseCaseIdPatternMatch(), expectedErrorMessagesNumber, maxWaitingMs, errorUseCases);
    }

    protected void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, this::normalizePayload);
    }

    protected void checkErrorMessageHeaders(String srcTopic,String group, ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey, boolean expectRetryHeader, boolean expectedAppNameHeader) {
        kafkaTestUtilitiesService.checkErrorMessageHeaders(srcTopic, group, errorMessage, errorDescription, expectedPayload, expectedKey, expectRetryHeader, expectedAppNameHeader, this::normalizePayload);
    }

    protected String normalizePayload(String expectedPayload) {
        String temp = TestUtils.truncateDateTimeField(expectedPayload, "elaborationDateTime");
        temp = TestUtils.setNullFieldValue(temp, "ruleEngineTopicPartition");
        temp = TestUtils.setNullFieldValue(temp, "ruleEngineTopicOffset");
        return TestUtils.truncateDateTimeField(temp,"timestamp");
    }
}
