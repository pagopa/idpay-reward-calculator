package it.gov.pagopa.reward.connector.event.consumer;

import it.gov.pagopa.reward.service.lookup.OnboardingOutcomeMediatorService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

@Configuration
@Slf4j
public class OnboardingConsumerConfig {
    @Bean
    public Consumer<Flux<Message<String>>> onboardingOutcome(
        OnboardingOutcomeMediatorService onboardingConsumer){
        return onboardingConsumer::execute;
    }
}
