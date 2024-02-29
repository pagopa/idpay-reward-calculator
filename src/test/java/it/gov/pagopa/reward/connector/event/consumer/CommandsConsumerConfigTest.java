package it.gov.pagopa.reward.connector.event.consumer;

import it.gov.pagopa.reward.service.commands.CommandsMediatorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class CommandsConsumerConfigTest {

    @Mock
    private CommandsMediatorService commandsMediatorServiceMock;


    @Test
    void commandsConsumerNotNull() {
        CommandsConsumerConfig consumerConfig = new CommandsConsumerConfig(commandsMediatorServiceMock);

        Consumer<Flux<Message<String>>> result = consumerConfig.commandsConsumer();

        Assertions.assertNotNull(result);

    }
}