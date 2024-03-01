package it.gov.pagopa.reward.connector.event.consumer;

import it.gov.pagopa.reward.service.lookup.HpanInitiativeMediatorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

class HpanInitiaveConsumerConfigTest {

    @Test
    void hpanInitiativeConsumerNotNull() {
        HpanInitiativeMediatorService hpanInitiativeMediatorServiceMock = Mockito.mock(HpanInitiativeMediatorService.class);
        HpanInitiaveConsumerConfig hpanInitiaveConsumerConfig = new HpanInitiaveConsumerConfig();

        Consumer<Flux<Message<String>>> result = hpanInitiaveConsumerConfig.hpanInitiativeConsumer(hpanInitiativeMediatorServiceMock);

        Assertions.assertNotNull(result);

    }
}