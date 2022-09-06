package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.reward.dto.HpanInitiativeDTO;
import it.gov.pagopa.reward.dto.mapper.HpanInitiativeDTO2InitialEntityMapper;
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
    void execute() throws JsonProcessingException{
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        HpanInitiativesService hpanInitiativesService = Mockito.mock(HpanInitiativesService.class);
        ErrorNotifierService errorNotifierService=Mockito.mock(ErrorNotifierService.class);
        HpanInitiativeDTO2InitialEntityMapper hpanInitiativeDTO2InitialEntityMapper = new HpanInitiativeDTO2InitialEntityMapper();
        HpanInitiativeMediatorService hpanInitiativeMediatorService = new HpanInitiativeMediatorServiceImpl(hpanInitiativesRepository, hpanInitiativesService, TestUtils.objectMapper,  errorNotifierService, hpanInitiativeDTO2InitialEntityMapper);

        //region input test

        HpanInitiativeDTO hpanInitiativeValidJson = HpanInitiativeDTOFaker.mockInstance(1);
        hpanInitiativeValidJson.setOperationType(HpanInitiativeConstants.ADD_INSTRUMENT);
        hpanInitiativeValidJson.setOperationDate(LocalDateTime.now().plusDays(10L));

        Mono<HpanInitiatives> hpanInitiatives1 = Mono.just(HpanInitiativesFaker.mockInstance(1));

        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeValidJson.getHpan())).thenReturn(hpanInitiatives1);

        Mockito.when(hpanInitiativesService.evaluate(hpanInitiativeValidJson,hpanInitiatives1.block()))
                .thenReturn(null);

        //No message without hpan
        HpanInitiativeDTO hpanInitiativeDTONotHpan = HpanInitiativeDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .operationType("DELETE_INSTRUMENT")
                .operationDate(LocalDateTime.now()).build();

        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTONotHpan.getHpan())).thenThrow(IllegalArgumentException.class);

        //No message without date
        HpanInitiativeDTO hpanInitiativeDTONotDate = HpanInitiativeDTO.builder()
                .userId("USERID")
                .hpan("HPAN")
                .initiativeId("INITIATIVEID")
                .operationType("DELETE_INSTRUMENT").build();

        Mockito.when(hpanInitiativesRepository.findById(hpanInitiativeDTONotDate.getHpan())).thenThrow(NullPointerException.class);

        //endregion

        HpanInitiatives hpanInitiativesOut = HpanInitiatives.builder()
                .hpan("HPAN_OUT")
                .userId("USERID_OUT").build();

        Mockito.when(hpanInitiativesService.evaluate(hpanInitiativeValidJson,hpanInitiatives1.block()))
                .thenReturn(hpanInitiativesOut);

        Mockito.when(hpanInitiativesRepository.save(Mockito.any(HpanInitiatives.class))).thenReturn(Mono.just(hpanInitiativesOut));

        ObjectMapper objectMapper = new ObjectMapper();
        // When
        hpanInitiativeMediatorService.execute(Flux
                .fromIterable(List.of(MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeValidJson)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTONotHpan)).build(),
                        MessageBuilder.withPayload(objectMapper.writeValueAsString(hpanInitiativeDTONotDate)).build(),
                        MessageBuilder.withPayload("NOT VALID JSON").build()
                )));

        // Then
        Mockito.verify(hpanInitiativesRepository,Mockito.times(2)).findById(Mockito.anyString());
        Mockito.verify(hpanInitiativesService,Mockito.times(1)).evaluate(Mockito.any(HpanInitiativeDTO.class),Mockito.any(HpanInitiatives.class));
        Mockito.verify(hpanInitiativesRepository,Mockito.times(1)).save(Mockito.any());
        Mockito.verify(errorNotifierService,Mockito.times(3)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(Throwable.class));
        Mockito.verify(errorNotifierService, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(IllegalArgumentException.class));
        Mockito.verify(errorNotifierService, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(NullPointerException.class));
        Mockito.verify(errorNotifierService, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));
    }
}

