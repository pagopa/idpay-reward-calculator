package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

class HpanInitiativeMediatorServiceImplTest {

    @Test
    void execute() throws JsonProcessingException, InterruptedException {
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        HpanInitiativesService hpanInitiativesService = Mockito.mock(HpanInitiativesService.class);
        ErrorNotifierService errorNotifierService=Mockito.mock(ErrorNotifierService.class);
        HpanInitiativeMediatorService hpanInitiativeMediatorService = new HpanInitiativeMediatorServiceImpl(hpanInitiativesRepository, hpanInitiativesService, TestUtils.objectMapper,  errorNotifierService);

        //region input test
        HpanInitiativeDTO hpanInitiativeDTO1 = HpanInitiativeDTOFaker.mockInstance(1);
        hpanInitiativeDTO1.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);
        hpanInitiativeDTO1.setOperationDate(LocalDateTime.now().plusDays(10L));

        HpanInitiativeDTO hpanInitiativeDTO2 = HpanInitiativeDTOFaker.mockInstance(2);
        hpanInitiativeDTO2.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);
        hpanInitiativeDTO2.setOperationDate(LocalDateTime.now().plusDays(10L));

        HpanInitiativeDTO hpanInitiativeDTO3 = HpanInitiativeDTOFaker.mockInstance(3);

        HpanInitiativeDTO hpanInitiativeDTO4 = HpanInitiativeDTOFaker.mockInstance(4);
        hpanInitiativeDTO4.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);
        hpanInitiativeDTO4.setOperationDate(LocalDateTime.now().plusDays(4L));

        HpanInitiativeDTO hpanInitiativeDTO5 = HpanInitiativeDTOFaker.mockInstance(5);
        hpanInitiativeDTO5.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);
        hpanInitiativeDTO5.setOperationDate(LocalDateTime.now().minusYears(1L).minusMonths(2L));

        HpanInitiativeDTO hpanInitiativeDTO6 = HpanInitiativeDTOFaker.mockInstance(6);
        hpanInitiativeDTO6.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);
        hpanInitiativeDTO6.setOperationDate(LocalDateTime.now().plusDays(4L));


        //Check this case
        HpanInitiativeDTO hpanInitiativeDTO7 = HpanInitiativeDTOFaker.mockInstance(7);
        hpanInitiativeDTO7.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);
        hpanInitiativeDTO7.setOperationDate(LocalDateTime.now().plusMonths(2L));

        HpanInitiativeDTO hpanInitiativeDTO8 = HpanInitiativeDTOFaker.mockInstance(8);
        hpanInitiativeDTO8.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);
        hpanInitiativeDTO8.setOperationDate(LocalDateTime.now().plusMonths(2L));

        HpanInitiativeDTO hpanInitiativeDTO9 = HpanInitiativeDTOFaker.mockInstance(9);
        hpanInitiativeDTO9.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);
        hpanInitiativeDTO9.setOperationDate(LocalDateTime.now().minusDays(4L));

        HpanInitiativeDTO hpanInitiativeDTO10 = HpanInitiativeDTOFaker.mockInstance(10);
        hpanInitiativeDTO10.setOperationType(HpanInitiativeConstants.DELETE_INSTRUMENT);
        hpanInitiativeDTO10.setOperationDate(LocalDateTime.now().minusYears(1L).minusMonths(2L));

        RewardTransactionDTO notValidDTO = RewardTransactionDTO.builder()
                .status("ACTIVE").mcc("4560").build();
        //endregion

        Mono<HpanInitiatives> hpanInitiatives1 = Mono.just(HpanInitiativesFaker.mockInstance(1));
        Mono<HpanInitiatives> hpanInitiatives2 = Mono.just(HpanInitiativesFaker.mockInstanceWithCloseIntervals(2));
        Mono<HpanInitiatives> hpanInitiatives4 = Mono.just(HpanInitiativesFaker.mockInstance(4));
        Mono<HpanInitiatives> hpanInitiatives5 = Mono.just(HpanInitiativesFaker.mockInstanceWithCloseIntervals(5));
        Mono<HpanInitiatives> hpanInitiatives6 = Mono.just(HpanInitiativesFaker.mockInstance(6));

        Mono<HpanInitiatives> hpanInitiatives7 = Mono.just(HpanInitiativesFaker.mockInstance(7));
        Mono<HpanInitiatives> hpanInitiatives8 = Mono.just(HpanInitiativesFaker.mockInstanceWithCloseIntervals(8));
        Mono<HpanInitiatives> hpanInitiatives9 = Mono.just(HpanInitiativesFaker.mockInstance(9));
        Mono<HpanInitiatives> hpanInitiatives10 = Mono.just(HpanInitiativesFaker.mockInstanceWithCloseIntervals(10));

        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO1.getHpan())).thenReturn(hpanInitiatives1);
        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO2.getHpan())).thenReturn(hpanInitiatives2);
//        Mockito.when(hpanInitiativesRepository.findById().thenThrow(IllegalArgumentException.class);
        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO4.getHpan())).thenReturn(hpanInitiatives4);
        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO5.getHpan())).thenReturn(hpanInitiatives5);
        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO6.getHpan())).thenReturn(hpanInitiatives6);
        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO7.getHpan())).thenReturn(hpanInitiatives7);
        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO8.getHpan())).thenReturn(hpanInitiatives8);
        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO9.getHpan())).thenReturn(hpanInitiatives9);
        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTO10.getHpan())).thenReturn(hpanInitiatives10);

        HpanInitiatives hpanInitiativesOut = HpanInitiatives.builder()
                .hpan("HPAN_OUT")
                .userId("USERID_OUT").build();

        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO1,hpanInitiatives1)))
                .thenReturn(Mono.just(hpanInitiativesOut));

        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO7,hpanInitiatives7)))
                .thenReturn(Mono.just(hpanInitiativesOut));
        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO2,hpanInitiatives2)))
                .thenReturn(Mono.just(hpanInitiativesOut));
        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO3,Mono.empty())))
                .thenReturn(Mono.just(hpanInitiativesOut));
        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO4,hpanInitiatives4)))
                .thenReturn(Mono.just(hpanInitiativesOut));
        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO5,hpanInitiatives5)))
                .thenReturn(Mono.just(hpanInitiativesOut));
        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO6,hpanInitiatives6)))
                .thenReturn(Mono.just(hpanInitiativesOut));
        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO8,hpanInitiatives8)))
                .thenReturn(Mono.just(hpanInitiativesOut));
        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO9,hpanInitiatives9)))
                .thenReturn(Mono.just(hpanInitiativesOut));
        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO10,hpanInitiatives10)))
                .thenReturn(Mono.just(hpanInitiativesOut));

        Mockito.when(hpanInitiativesService.hpanInitiativeUpdateInformation(Pair.of(hpanInitiativeDTO7,hpanInitiatives7)))
                .thenReturn(Mono.just(hpanInitiativesOut));

        Mockito.when(hpanInitiativesRepository.save(Mockito.any(HpanInitiatives.class))).thenReturn(Mono.just(hpanInitiativesOut));

        ObjectMapper objectMapper = new ObjectMapper();
        // When
        hpanInitiativeMediatorService.execute(Flux
                .fromIterable(List.of(MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTO1)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTO2)).build(),
//                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTO3)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTO4)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTO5)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTO6)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTO7)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTO8)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTO9)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTO10)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(notValidDTO)).build()
                )));

        // Then
        Mockito.verify(hpanInitiativesRepository,Mockito.times(9)).findById(Mockito.anyString());
        Mockito.verify(hpanInitiativesService,Mockito.times(9)).hpanInitiativeUpdateInformation(Mockito.any());
        Mockito.verify(hpanInitiativesRepository,Mockito.times(9)).save(Mockito.any());

    }
}