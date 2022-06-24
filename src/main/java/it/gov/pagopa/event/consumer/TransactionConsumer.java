package it.gov.pagopa.event.consumer;

import it.gov.pagopa.dto.mapper.RewardsTransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class TransactionConsumer {

    @Bean
    public Consumer<Flux<RewardsTransactionDTO>> trxConsumerReward(){
        return trxFlux -> trxFlux.subscribe(
                trx -> log.info("Transaction Reward: {}", trx)
        );
    }
}
