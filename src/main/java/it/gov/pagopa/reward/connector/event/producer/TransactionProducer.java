package it.gov.pagopa.reward.connector.event.producer;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Supplier;

@Configuration
@Slf4j
public class TransactionProducer {

    @Bean
    public Sinks.Many<TransactionDTO> trxMany(){
        return Sinks.many().unicast().onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<TransactionDTO>> trxProducer(Sinks.Many<TransactionDTO> many) {
        return () -> many.asFlux()
                .doOnNext(trx -> log.info("Sending message: {}", trx))
                .doOnError(e -> log.error("Error encountered", e));
    }
}
