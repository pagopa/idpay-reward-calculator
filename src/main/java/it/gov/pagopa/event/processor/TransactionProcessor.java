package it.gov.pagopa.event.processor;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.model.RewardTransaction;
import it.gov.pagopa.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Configuration
@Slf4j
public class TransactionProcessor {

    private final TransactionService trxService;

    public TransactionProcessor(TransactionService trxService){
        this.trxService = trxService;
    }

    @Bean
    public Function<Flux<TransactionDTO>,Flux<RewardTransaction>> trxProcessor(){
        return transactionsFlux ->  transactionsFlux
                .map(this.trxService::applyRules);
    }


}
