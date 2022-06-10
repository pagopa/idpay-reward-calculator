package it.gov.pagopa.event.consumer;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.model.TransactionPrize;
import it.gov.pagopa.dto.mapper.TransactionMapper;
import it.gov.pagopa.service.jpa.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Componente che lancia la regola precedentemente configurata allo StartUP
 */
@Component
@Slf4j
public class TrxConsumer implements Consumer<Flux<TransactionDTO>> {
    private final KieContainer kieContainer;
    private final TransactionMapper trxMapper;
    private final TransactionService trxService;
    private KieSession kieSession;

    public TrxConsumer(KieContainer kieContainer, TransactionMapper trxMapper, TransactionService trxService) {
        this.kieContainer = kieContainer;
        this.trxMapper = trxMapper;
        this.trxService = trxService;
    }

    @PostConstruct
    public void init() {
        kieSession = kieContainer.newKieSession();
    }

    @Override
    public void accept(Flux<TransactionDTO> valueFlux) {

        valueFlux.subscribe(trx ->  {
                Instant before=Instant.now();
                //ritrovamento delle inizitive dipendendo dal suo hpan // da un DB oppure da un altro microservizio (restTesplate)

                applyRules()
                .apply(trxMapper.map(trx))
                .flatMap(this.trxService::save)
                .subscribe(t -> log.info("Save transaction into DB: {}",t));
                Instant after = Instant.now();
                log.info("Total Time to consume & ApplyRules: {} ms", Duration.between(before, after).toMillis());
        });

    }
    private Function<TransactionPrize,Mono<TransactionPrize>> applyRules() {
        return t -> {
            Instant before;
            Instant after;
            kieSession.insert(t);
            before = Instant.now();
            kieSession.fireAllRules();
            after = Instant.now();
            log.info("Time between before and after fireAllRules: {} ms", Duration.between(before, after).toMillis());
            return Mono.just(t);
        };
    }
}
