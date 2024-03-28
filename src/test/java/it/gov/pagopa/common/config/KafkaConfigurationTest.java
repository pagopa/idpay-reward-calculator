package it.gov.pagopa.common.config;

import it.gov.pagopa.reward.config.KafkaConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

@TestPropertySource( properties = {
        "spring.cloud.stream.binders.binder-test.type=kafka",
        "spring.cloud.stream.binders.binder-test.environment.spring.cloud.stream.kafka.binder.brokers=BROKERTEST",
        "spring.cloud.stream.bindings.binding-test-in-0.destination=topic_test",
        "spring.cloud.stream.bindings.binding-test-in-0.binder=binder-test",
        "spring.cloud.stream.bindings.binding-test-in-0.group=group-test",
        "spring.cloud.stream.bindings.binding-test-without-binder-in-0.destination=topic_test-without-binder",
        "spring.cloud.stream.bindings.binding-test-without-binder-in-0.binder=unexpected-binder",
        "spring.cloud.stream.binders.binder-test-without-environment.type=kafka",
        "spring.cloud.stream.bindings.binding-test-without-environment-in-0.destination=topic_test-without-environment",
        "spring.cloud.stream.bindings.binding-test-without-environment-in-0.binder=binder-test-without-environment"
})
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = KafkaConfiguration.class)
class KafkaConfigurationTest {

    @Value("${spring.cloud.stream.binders.binder-test.type}")
    private String binderTestType;
    @Value("${spring.cloud.stream.binders.binder-test.environment.spring.cloud.stream.kafka.binder.brokers}")
    private String binderTestBroker;
    @Value("${spring.cloud.stream.bindings.binding-test-in-0.destination}")
    private String binderTestTopic;
    @Value("${spring.cloud.stream.bindings.binding-test-in-0.binder}")
    private String binderTestName;
    @Value("${spring.cloud.stream.bindings.binding-test-in-0.group}")
    private String binderTestGroup;
    @Value("${spring.cloud.stream.bindings.binding-test-without-binder-in-0.destination}")
    private String withoutBinderTopic;
    @Value("${spring.cloud.stream.bindings.binding-test-without-binder-in-0.binder}")
    private String withoutBinderName;
    @Value("${spring.cloud.stream.binders.binder-test-without-environment.type}")
    private String withoutEnvironmentType;
    @Value("${spring.cloud.stream.bindings.binding-test-without-environment-in-0.destination}")
    private String withoutEnvironmentTopic;
    @Value("${spring.cloud.stream.bindings.binding-test-without-environment-in-0.binder}")
    private String withoutEnvironmentName;
    @Autowired
    private KafkaConfiguration config;



    @Test
    void getStream() {
        Map<String, KafkaConfiguration.KafkaInfoDTO> bindings = config.getStream().getBindings();
        Assertions.assertEquals(2, bindings.size());

        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = bindings.get("binding-test-in-0");
        Assertions.assertNotNull(kafkaInfoDTO);
        Assertions.assertEquals(binderTestType, kafkaInfoDTO.getType());
        Assertions.assertEquals(binderTestTopic, kafkaInfoDTO.getDestination());
        Assertions.assertEquals(binderTestGroup, kafkaInfoDTO.getGroup());
        Assertions.assertEquals(binderTestBroker, kafkaInfoDTO.getBrokers());
        Assertions.assertEquals(binderTestName, kafkaInfoDTO.getBinder());

        Map<String, KafkaConfiguration.Binders> binders = config.getStream().getBinders();
        Assertions.assertEquals(1, binders.size());
        KafkaConfiguration.Binders binderDTO = binders.get(binderTestName);
        Assertions.assertEquals(binderTestType, binderDTO.getType());
        Assertions.assertEquals(binderTestBroker, binderDTO.getEnvironment().getSpring().getCloud().getStream().getKafka().getBinder().getBrokers());
    }

    @Test
    void getStreamWithoutBinders() {
        Map<String, KafkaConfiguration.KafkaInfoDTO> bindings = config.getStream().getBindings();
        Assertions.assertEquals(2, bindings.size());

        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = bindings.get("binding-test-without-binder-in-0");
        Assertions.assertNotNull(kafkaInfoDTO);
        Assertions.assertEquals(withoutBinderTopic, kafkaInfoDTO.getDestination());
        Assertions.assertNull(kafkaInfoDTO.getGroup());
        Assertions.assertEquals(withoutBinderName,kafkaInfoDTO.getBinder());
        Assertions.assertNull(kafkaInfoDTO.getType());
        Assertions.assertNull(kafkaInfoDTO.getBrokers());

        Map<String, KafkaConfiguration.Binders> binders = config.getStream().getBinders();
        Assertions.assertEquals(1, binders.size());
        KafkaConfiguration.Binders binderDTO = binders.get(withoutBinderName);
        Assertions.assertNull(binderDTO);
    }

    @Test
    void getStreamWithoutEnvironment() {
        Map<String, KafkaConfiguration.KafkaInfoDTO> bindings = config.getStream().getBindings();
        Assertions.assertEquals(3, bindings.size());
        KafkaConfiguration.KafkaInfoDTO kafkaInfoDTO = bindings.get("binding-test-without-environment-in-0");
        Assertions.assertNotNull(kafkaInfoDTO);
        Assertions.assertEquals(withoutEnvironmentTopic, kafkaInfoDTO.getDestination());
        Assertions.assertNull(kafkaInfoDTO.getGroup());
        Assertions.assertEquals(withoutEnvironmentName, kafkaInfoDTO.getBinder());
        Assertions.assertEquals(withoutEnvironmentType, kafkaInfoDTO.getType());
        Assertions.assertNull(kafkaInfoDTO.getBrokers());
        Map<String, KafkaConfiguration.Binders> binders = config.getStream().getBinders();
        Assertions.assertEquals(2, binders.size());
        KafkaConfiguration.Binders binderDTO = binders.get(withoutEnvironmentName);
        Assertions.assertEquals(withoutEnvironmentType, binderDTO.getType());
        Assertions.assertNull(binderDTO.getEnvironment());
    }

}