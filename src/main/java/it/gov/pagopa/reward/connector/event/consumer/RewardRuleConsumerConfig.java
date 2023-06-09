package it.gov.pagopa.reward.connector.event.consumer;

import it.gov.pagopa.reward.service.build.RewardRuleMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class RewardRuleConsumerConfig {
    @Bean
    public Consumer<Flux<Message<String>>> rewardRuleConsumer(RewardRuleMediatorService rewardRuleMediatorService){
        return rewardRuleMediatorService::execute;
    }
}
