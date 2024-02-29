package it.gov.pagopa.reward.service.lookup;

import com.mongodb.assertions.Assertions;
import it.gov.pagopa.reward.dto.HpanUpdateOutcomeDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

class HpanUpdateNotifierServiceImplTest {

    @Test
    void hpanProducerConfigNotNull() {
        HpanUpdateNotifierServiceImpl.HpanUpdateNotifierProducerConfig hpanUpdateNotifierProducerConfig = new HpanUpdateNotifierServiceImpl.HpanUpdateNotifierProducerConfig();

        Supplier<Flux<Message<HpanUpdateOutcomeDTO>>> result = hpanUpdateNotifierProducerConfig.hpanUpdateOutcome();

        Assertions.assertNotNull(result);
    }
    @Test
    void testNotify() {
        StreamBridge streamBridgeMock = Mockito.mock(StreamBridge.class);

        HpanUpdateNotifierServiceImpl hpanUpdateNotifierService = new HpanUpdateNotifierServiceImpl(streamBridgeMock);

        HpanUpdateOutcomeDTO outcomeDTO = HpanUpdateOutcomeDTO.builder().build();

        Mockito.when(streamBridgeMock.send(Mockito.eq("hpanUpdateOutcome-out-0"), Mockito.any()))
                .thenReturn(true);


        boolean result = hpanUpdateNotifierService.notify(outcomeDTO);

        Assertions.assertTrue(result);
        Mockito.verify(streamBridgeMock, Mockito.only()).send(Mockito.any(), Mockito.any());
    }

}