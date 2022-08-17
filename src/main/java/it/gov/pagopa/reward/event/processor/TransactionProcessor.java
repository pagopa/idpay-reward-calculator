package it.gov.pagopa.reward.event.processor;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.service.reward.RewardCalculatorMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Configuration
@Slf4j
public class TransactionProcessor {

    private final RewardCalculatorMediatorService rewardCalculatorMediatorService;

    public TransactionProcessor(RewardCalculatorMediatorService rewardCalculatorMediatorService){
        this.rewardCalculatorMediatorService = rewardCalculatorMediatorService;
    }

    /**
     * Read from the topic ${KAFKA_TOPIC_RTD_TRX} and publish to topic ${KAFKA_TOPIC_REWARD_TRX}
     * */
    @Bean
    public Function<Flux<Message<String>>,Flux<RewardTransactionDTO>> trxProcessor(){
        return rewardCalculatorMediatorService::execute;
    }

}
