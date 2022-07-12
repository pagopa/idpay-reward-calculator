package it.gov.pagopa.event.processor;

import it.gov.pagopa.dto.RewardTransactionDTO;
import it.gov.pagopa.dto.TransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Slf4j
@EmbeddedKafka(partitions = 1, controlledShutdown = true, topics = {"rtd-trx","idpay-reward-calculator-response"})
@TestPropertySource(
        properties = {
                "spring.cloud.stream.kafka.binder.zkNodes=${spring.embedded.zookeeper.connect}",
                "spring.cloud.stream.binders.kafka-rtd.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-idpay.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.binders.kafka-rtd-producer.environment.spring.cloud.stream.kafka.binder.brokers=${spring.embedded.kafka.brokers}",
                "spring.cloud.stream.bindings.trxProcessor-in-0.destination=rtd-trx",
                "spring.cloud.stream.bindings.trxProcessor-out-0.destination=idpay-reward-calculator-response",
                "spring.cloud.stream.bindings.trxProcessor-in-0.group=idpay-group",
                "spring.cloud.stream.bindings.trxProducer-out-0.destination=rtd-trx",
                "spring.cloud.stream.kafka.binder.configuration.security.protocol=PLAINTEXT"
        })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransactionProcessorTest {

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    KafkaTemplate<String, TransactionDTO> kafkaTemplate;

    Consumer<String, RewardTransactionDTO> consumer;

    @BeforeEach
    void setup() {
        Map<String, Object> producerConfig = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfig));

        Map<String,Object> consumerConfig = KafkaTestUtils.consumerProps("idpay-group-rewards","true",embeddedKafkaBroker);
        consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG,"idpay-group-rewards");
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerConfig.put(JsonDeserializer.USE_TYPE_INFO_HEADERS,"false");
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, RewardTransactionDTO> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerConfig,new StringDeserializer(),new JsonDeserializer<>(RewardTransactionDTO.class));
        consumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer,"idpay-reward-calculator-response");

    }

    @Test
    void testTrxProcessor() {
        // Given
        TransactionDTO trx = TransactionDTO.builder()
                .idTrxAcquirer("98174002165501220007165503")
                .acquirerCode("36081")
                .trxDate(OffsetDateTime.parse("2020-09-07T15:58:42.000+00:00"))
                .hpan("5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0")
                .operationType("00")
                .amount(new BigDecimal("200.00"))
                .acquirerId("09509")
                .build();

        kafkaTemplate.send("rtd-trx", trx);

        //Then
        consumer.seekToBeginning(consumer.assignment());

        for(int i=0, j=0; i<=getExpectedPublishedMessagesCount() && j<=getExpectedPublishedMessagesCount(); j++){
            ConsumerRecords<String,RewardTransactionDTO> publish = consumer.poll(Duration.ofMillis(7000));
            for (ConsumerRecord<String, RewardTransactionDTO> record: publish) {
                Assertions.assertNotNull(record.value().getRewards());
                Assertions.assertNotNull(record.value().getInitiatives());
            }
        }
    }

    int getExpectedPublishedMessagesCount() {
        return 1;
    }
}






