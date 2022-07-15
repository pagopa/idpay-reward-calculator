package it.gov.pagopa.reward.event.consumer;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class TransactionConsumer {

    @Bean
    public Consumer<Flux<RewardTransactionDTO>> trxConsumerReward(){
        return trxFlux -> trxFlux.subscribe(
                trx -> log.info("Transaction Reward: {}", trx)
        );
    }
}
