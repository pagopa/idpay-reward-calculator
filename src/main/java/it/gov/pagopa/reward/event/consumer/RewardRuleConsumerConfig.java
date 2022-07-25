package it.gov.pagopa.reward.event.consumer;

import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.service.build.RewardRuleMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class RewardRuleConsumerConfig {
    @Bean
    public Consumer<Flux<InitiativeReward2BuildDTO>> rewardRuleConsumer(RewardRuleMediatorService rewardRuleMediatorService){
        return rewardRuleMediatorService::execute;
    }
}
