package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2InitialEntityMapper;
import it.gov.pagopa.reward.dto.mapper.HpanUpdateBulk2SingleMapper;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

class HpanInitiativeMediatorServiceImplTest {

    @Test
    void execute(){
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        HpanInitiativesService hpanInitiativesService = Mockito.mock(HpanInitiativesService.class);
        ErrorNotifierService errorNotifierService=Mockito.mock(ErrorNotifierService.class);
        HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper = new HpanInitiativeDTO2InitialEntityMapper();
        HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper = new HpanUpdateBulk2SingleMapper();
        HpanInitiativeMediatorService hpanInitiativeMediatorService = new HpanInitiativeMediatorServiceImpl(hpanInitiativesRepository, hpanInitiativesService, TestUtils.objectMapper,  errorNotifierService, hpanInitiativeDTO2InitialEntityMapper, hpanUpdateBulk2SingleMapper);

        //region input test
        HpanInitiativeBulkDTO hpanInitiativeBulkDTOValidJson = HpanInitiativeBulkDTO.builder()
                .userId("USERID")
                .hpanList(List.of("HPAN_1"))
                .initiativeId("INITIATIVEID")
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now().plusDays(10L)).build();

        Mono<HpanInitiatives> hpanInitiatives1 = Mono.just(HpanInitiativesFaker.mockInstance(1));
        Mockito.when(hpanInitiativesRepository.findById("HPAN_1")).thenReturn(hpanInitiatives1);

        // Message without hpan
        HpanInitiativeBulkDTO hpanInitiativeBulkDTONotHpanList = HpanInitiativeBulkDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .operationType(HpanInitiativeConstants.DELETE_INSTRUMENT)
                .operationDate(LocalDateTime.now().plusDays(10L)).build();

        //Message without date
        HpanInitiativeBulkDTO hpanInitiativeBulkDTONotDate = HpanInitiativeBulkDTO.builder()
                .userId("USERID")
                .hpanList(List.of("HPAN_2"))
                .initiativeId("INITIATIVEID")
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT).build();

        HpanInitiatives hpanInitiatives2 = HpanInitiativesFaker.mockInstance(2);

        Mockito.when(hpanInitiativesRepository.findById("HPAN_2")).thenReturn( Mono.just(hpanInitiatives2));
        Mockito.when(hpanInitiativesService.evaluate(Mockito.any(),Mockito.same(hpanInitiatives2))).thenThrow(NullPointerException.class);

        //endregion
        HpanInitiatives hpanInitiativesOut = HpanInitiatives.builder()
                .hpan("HPAN_OUT")
                .userId("USERID_OUT").build();

        Mockito.when(hpanInitiativesService.evaluate(hpanUpdateBulk2SingleMapper.apply(hpanInitiativeBulkDTOValidJson,"HPAN_1"),hpanInitiatives1.block())).thenReturn(hpanInitiativesOut);
        Mockito.when(hpanInitiativesRepository.save(Mockito.any(HpanInitiatives.class))).thenReturn(Mono.just(hpanInitiativesOut));

        // When
        hpanInitiativeMediatorService.execute(Flux
               .fromIterable(List.of(MessageBuilder.withPayload(TestUtils.jsonSerializer(hpanInitiativeBulkDTOValidJson)).build(),
                        MessageBuilder.withPayload(TestUtils.jsonSerializer(hpanInitiativeBulkDTONotHpanList)).build(),
                        MessageBuilder.withPayload(TestUtils.jsonSerializer(hpanInitiativeBulkDTONotDate)).build(),
                        MessageBuilder.withPayload("NOT VALID JSON").build()
                )));
        // Then
        Mockito.verify(hpanInitiativesRepository,Mockito.times(2)).findById(Mockito.anyString());
        Mockito.verify(hpanInitiativesService,Mockito.times(2)).evaluate(Mockito.any(HpanInitiativeDTO.class),Mockito.any(HpanInitiatives.class));
        Mockito.verify(hpanInitiativesRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(errorNotifierService,Mockito.times(3)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(Throwable.class));
        Mockito.verify(errorNotifierService, Mockito.times(2)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(NullPointerException.class));
        Mockito.verify(errorNotifierService, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));
    }
}

