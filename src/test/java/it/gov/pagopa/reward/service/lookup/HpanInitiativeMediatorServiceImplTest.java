package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HpanInitiativeMediatorServiceImplTest {

    @Test
    void execute() {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        HpanInitiativesService hpanInitiativesService = Mockito.mock(HpanInitiativesService.class);
        HpanInitiativeMediatorService hpanInitiativeMediatorService = new HpanInitiativeMediatorServiceImpl(hpanInitiativesRepository, hpanInitiativesService);

        HpanInitiativeDTO hpanInitiativeDTO1 = HpanInitiativeDTOFaker.mockInstance(1);
        hpanInitiativeDTO1.setOperationType(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name());
        hpanInitiativeDTO1.setOperationDate(LocalDateTime.now().plusDays(10L));
        HpanInitiativeDTO hpanInitiativeDTO2 = HpanInitiativeDTOFaker.mockInstance(2);
        hpanInitiativeDTO2.setOperationType(HpanInitiativeDTO.OperationType.ADD_INSTRUMENT.name());
        hpanInitiativeDTO2.setOperationDate(LocalDateTime.now());
        HpanInitiativeDTO hpanInitiativeDTO3 = HpanInitiativeDTOFaker.mockInstance(3);

        HpanInitiatives hpanInitiatives1 = HpanInitiativesFaker.mockInstance(1);
        HpanInitiatives hpanInitiatives2 = HpanInitiativesFaker.mockInstanceWithCloseIntervals(2);

        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO1.getHpan())).thenReturn(Mono.just(hpanInitiatives1));
        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO2.getHpan())).thenReturn(Mono.just(hpanInitiatives2));
        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO3.getHpan())).thenReturn(Mono.empty());

        HpanInitiatives hpanInitiativesOut = Mockito.mock(HpanInitiatives.class);

       // Mockito.when(hpanInitiativesRepository.save(Mockito.any(HpanInitiatives.class))).thenReturn(Mono.just(hpanInitiativesOut));

        // When
        hpanInitiativeMediatorService.execute(Flux
                .fromIterable(List.of(MessageBuilder.withPayload(hpanInitiativeDTO1).build(),
                        MessageBuilder.withPayload(hpanInitiativeDTO2).build(),
                        MessageBuilder.withPayload(hpanInitiativeDTO3).build())));

        // Then
       // Mockito.verify(hpanInitiativesRepository, Mockito.times(2)).save(Mockito.any());
    }
}