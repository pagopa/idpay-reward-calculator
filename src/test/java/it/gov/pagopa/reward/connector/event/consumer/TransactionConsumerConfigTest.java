package it.gov.pagopa.reward.connector.event.consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

class TransactionConsumerConfigTest {

    @Test
    void trxResponseConsumerConfigNotNull() {
        TransactionConsumer transactionConsumerMock = Mockito.mock(TransactionConsumer.class);
        TransactionConsumerConfig consumerConfig = new TransactionConsumerConfig();

        Consumer<Flux<Message<String>>> result = consumerConfig.trxResponseConsumer(transactionConsumerMock);

        Assertions.assertNotNull(result);
    }
}