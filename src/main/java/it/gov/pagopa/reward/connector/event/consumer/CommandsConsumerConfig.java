package it.gov.pagopa.reward.connector.event.consumer;

import it.gov.pagopa.reward.service.commands.CommandsMediatorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class CommandsConsumerConfig {
    private final CommandsMediatorService commandsMediatorService;

    public CommandsConsumerConfig(CommandsMediatorService commandsMediatorService) {
        this.commandsMediatorService = commandsMediatorService;
    }


    @Bean
    public Consumer<Flux<Message<String>>> commandsConsumer(){
        return commandsMediatorService::execute;
    }
}
