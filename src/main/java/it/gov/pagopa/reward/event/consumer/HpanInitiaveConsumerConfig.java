package it.gov.pagopa.reward.event.consumer;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.service.lookup.HpanInitiativeMediatorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
public class HpanInitiaveConsumerConfig {

    @Bean
    public Consumer<Flux<HpanInitiativeDTO>> hpanInitiativeConsumer(HpanInitiativeMediatorService hpanInitiativeMediatorService){
        return hpanInitiativeMediatorService::execute;
    }
}
