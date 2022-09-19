package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.dto.mapper.HpanUpdateEvaluateDTO2HpanInitiativeMapper;
import it.gov.pagopa.reward.dto.mapper.HpanUpdateBulk2SingleMapper;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

class HpanInitiativeMediatorServiceImplTest {

    @Test
    void execute(){
        // Given
        HpanInitiativesRepository hpanInitiativesRepository = Mockito.mock(HpanInitiativesRepository.class);
        HpanInitiativesService hpanInitiativesService = Mockito.mock(HpanInitiativesService.class);
        ErrorNotifierService errorNotifierService=Mockito.mock(ErrorNotifierService.class);
        HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapper = new HpanUpdateEvaluateDTO2HpanInitiativeMapper();
        HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapper = new HpanUpdateBulk2SingleMapper();
        HpanInitiativeMediatorService hpanInitiativeMediatorService = new HpanInitiativeMediatorServiceImpl(0L, hpanInitiativesRepository, hpanInitiativesService, TestUtils.objectMapper,  errorNotifierService, hpanUpdateEvaluateDTO2HpanInitiativeMapper, hpanUpdateBulk2SingleMapper);

        //region input test
        String hpanValid = "HPAN_VALID";
        Acknowledgment hpanInitiativeBulkDTOValidJsonAck = Mockito.mock(Acknowledgment.class);
        HpanInitiativeBulkDTO hpanInitiativeBulkDTOValidJson = HpanInitiativeBulkDTO.builder()
                .userId("USERID")
                .hpanList(List.of(hpanValid))
                .initiativeId("INITIATIVEID")
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now().plusDays(10L)).build();

        HpanInitiatives hpanInitiatives1 = HpanInitiativesFaker.mockInstance(1);
        Mockito.when(hpanInitiativesRepository.findById(hpanValid)).thenReturn(Mono.just(hpanInitiatives1));

        // Message without hpan
        Acknowledgment hpanInitiativeBulkDTONotHpanListAck = Mockito.mock(Acknowledgment.class);
        HpanInitiativeBulkDTO hpanInitiativeBulkDTONotHpanList = HpanInitiativeBulkDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .operationType(HpanInitiativeConstants.DELETE_INSTRUMENT)
                .operationDate(LocalDateTime.now().plusDays(10L)).build();

        //Message without date
        String hpanNotDate = "HPAN_NOT_DATE";
        Acknowledgment hpanInitiativeBulkDTONotDateAck = Mockito.mock(Acknowledgment.class);
        HpanInitiativeBulkDTO hpanInitiativeBulkDTONotDate = HpanInitiativeBulkDTO.builder()
                .userId("USERID")
                .hpanList(List.of(hpanNotDate))
                .initiativeId("INITIATIVEID")
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT).build();

        HpanInitiatives hpanInitiatives2 = HpanInitiativesFaker.mockInstance(2);
        Mockito.when(hpanInitiativesRepository.findById(hpanNotDate)).thenReturn(Mono.just(hpanInitiatives2));

        //endregion
        OnboardedInitiative onboardedInitiativeOut1 = OnboardedInitiative.builder().initiativeId("INITIATIVEID").build();

        OnboardedInitiative onboardedInitiativeOut2 = OnboardedInitiative.builder().initiativeId("INITIATIVEID").build();

        Mockito.when(hpanInitiativesService.evaluate(Mockito.any(HpanUpdateEvaluateDTO.class),Mockito.same(hpanInitiatives1)))
                .thenReturn(onboardedInitiativeOut1);
        Mockito.when(hpanInitiativesService.evaluate(Mockito.any(HpanUpdateEvaluateDTO.class),Mockito.same(hpanInitiatives2)))
                .thenReturn(onboardedInitiativeOut2);

        UpdateResult updateResult = Mockito.mock(UpdateResult.class);
        Mockito.when(hpanInitiativesRepository.setInitiative(Mockito.anyString(), Mockito.any(OnboardedInitiative.class))).thenReturn(Mono.just(updateResult));

        Acknowledgment notValidJsonAck = Mockito.mock(Acknowledgment.class);

        // When
        hpanInitiativeMediatorService.execute(Flux
               .fromIterable(List.of(MessageBuilder.withPayload(TestUtils.jsonSerializer(hpanInitiativeBulkDTOValidJson)).copyHeaders(Map.of(KafkaHeaders.ACKNOWLEDGMENT, hpanInitiativeBulkDTOValidJsonAck)).build(),
                        MessageBuilder.withPayload(TestUtils.jsonSerializer(hpanInitiativeBulkDTONotHpanList)).copyHeaders(Map.of(KafkaHeaders.ACKNOWLEDGMENT, hpanInitiativeBulkDTONotHpanListAck)).build(),
                        MessageBuilder.withPayload(TestUtils.jsonSerializer(hpanInitiativeBulkDTONotDate)).copyHeaders(Map.of(KafkaHeaders.ACKNOWLEDGMENT, hpanInitiativeBulkDTONotDateAck)).build(),
                        MessageBuilder.withPayload("NOT VALID JSON").copyHeaders(Map.of(KafkaHeaders.ACKNOWLEDGMENT, notValidJsonAck)).build()
                )));
        // Then
        Mockito.verify(hpanInitiativesRepository,Mockito.times(2)).findById(Mockito.anyString());
        Mockito.verify(hpanInitiativesService,Mockito.times(2)).evaluate(Mockito.any(HpanUpdateEvaluateDTO.class),Mockito.any(HpanInitiatives.class));
        Mockito.verify(hpanInitiativesRepository, Mockito.times(2)).setInitiative(Mockito.anyString(), Mockito.any());
        Mockito.verify(errorNotifierService,Mockito.times(2)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(Throwable.class));
        Mockito.verify(errorNotifierService, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(NullPointerException.class));
        Mockito.verify(errorNotifierService, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));
    }
}