package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import it.gov.pagopa.reward.dto.mapper.HpanUpdateEvaluateDTO2HpanInitiativeMapper;
import it.gov.pagopa.reward.dto.mapper.HpanUpdateBulk2SingleMapper;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeBulkDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@ExtendWith(MockitoExtension.class)
class HpanInitiativeMediatorServiceImplTest {

    @Mock
    HpanInitiativesRepository hpanInitiativesRepositoryMock;
    @Mock
    HpanInitiativesService hpanInitiativesServiceMock;
    @Mock
    ErrorNotifierService errorNotifierServiceMock;
    @Mock
    HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapperMock;
    @Mock
    HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapperMock;

    HpanInitiativeMediatorService hpanInitiativeMediatorService;
    @BeforeEach
    void setUp(){
        hpanInitiativeMediatorService = new HpanInitiativeMediatorServiceImpl("appName",0L, hpanInitiativesRepositoryMock, hpanInitiativesServiceMock, TestUtils.objectMapper, errorNotifierServiceMock, hpanUpdateEvaluateDTO2HpanInitiativeMapperMock, hpanUpdateBulk2SingleMapperMock);
    }
    @Test
    void execute(){
        // Given
        //region input test
        String hpanValid = "HPAN_VALID";
        Acknowledgment hpanInitiativeBulkDTOValidJsonAck = Mockito.mock(Acknowledgment.class);

        PaymentMethodInfoDTO infoHpan = PaymentMethodInfoDTO.builder()
                .hpan(hpanValid)
                .maskedPan("MASKEDPAN_HPAN_VALID")
                .brandLogo("BRANDLOGO_HPAN_VALID").build();
        HpanInitiativeBulkDTO hpanInitiativeBulkDTOValidJson = HpanInitiativeBulkDTO.builder()
                .userId("USERID_HPAN_VALID")
                .infoList(List.of(infoHpan))
                .initiativeId("INITIATIVEID_HPAN_VALID")
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now().plusDays(10L)).build();

        HpanUpdateEvaluateDTO hpanUpdateValidHpan = HpanUpdateEvaluateDTO.builder()
                .userId(hpanInitiativeBulkDTOValidJson.getUserId())
                .initiativeId(hpanInitiativeBulkDTOValidJson.getInitiativeId())
                .hpan(infoHpan.getHpan())
                .maskedPan(infoHpan.getMaskedPan())
                .brandLogo(infoHpan.getBrandLogo())
                .operationType(hpanInitiativeBulkDTOValidJson.getOperationType())
                .evaluationDate(LocalDateTime.now())
                .build();
        Mockito.when(hpanUpdateBulk2SingleMapperMock.apply(hpanInitiativeBulkDTOValidJson,infoHpan)).thenReturn(hpanUpdateValidHpan);

        HpanInitiatives hpanInitiatives1 = HpanInitiativesFaker.mockInstance(1);
        hpanInitiatives1.setHpan(hpanValid);
        Mockito.when(hpanInitiativesRepositoryMock.findById(hpanValid)).thenReturn(Mono.just(hpanInitiatives1));


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

        PaymentMethodInfoDTO infoHpanNotDate = PaymentMethodInfoDTO.builder()
                .hpan(hpanNotDate)
                .maskedPan("MASKEDPAN_HPAN_NOT_DATE")
                .brandLogo("BRANDLOGO_HPAN_NOT_DATE").build();
        HpanInitiativeBulkDTO hpanInitiativeBulkDTONotDate = HpanInitiativeBulkDTO.builder()
                .userId("USERID_HPAN_NOT_DATE")
                .infoList(List.of(infoHpanNotDate))
                .initiativeId("INITIATIVEID_HPAN_NOT_DATE")
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT).build();

        HpanUpdateEvaluateDTO hpanUpdateNotDate = HpanUpdateEvaluateDTO.builder()
                .userId(hpanInitiativeBulkDTONotDate.getUserId())
                .initiativeId(hpanInitiativeBulkDTONotDate.getInitiativeId())
                .hpan(infoHpanNotDate.getHpan())
                .maskedPan(infoHpanNotDate.getMaskedPan())
                .brandLogo(infoHpanNotDate.getBrandLogo())
                .operationType(hpanInitiativeBulkDTONotDate.getOperationType())
                .evaluationDate(LocalDateTime.now())
                .build();
        Mockito.when(hpanUpdateBulk2SingleMapperMock.apply(hpanInitiativeBulkDTONotDate,infoHpanNotDate)).thenReturn(hpanUpdateNotDate);

        HpanInitiatives hpanInitiatives2 = HpanInitiativesFaker.mockInstance(2);
        hpanInitiatives2.setHpan(hpanNotDate);
        Mockito.when(hpanInitiativesRepositoryMock.findById(hpanNotDate)).thenReturn(Mono.just(hpanInitiatives2));

        //endregion
        OnboardedInitiative onboardedInitiativeOut1 = OnboardedInitiative.builder().initiativeId("INITIATIVEID_HPAN_VALID").build();

        OnboardedInitiative onboardedInitiativeOut2 = OnboardedInitiative.builder().initiativeId("INITIATIVEID_HPAN_NOT_DATE").build();

        Mockito.when(hpanInitiativesServiceMock.evaluate(hpanUpdateValidHpan,hpanInitiatives1))
                .thenReturn(onboardedInitiativeOut1);
        Mockito.when(hpanInitiativesServiceMock.evaluate(hpanUpdateNotDate,hpanInitiatives2))
                .thenReturn(onboardedInitiativeOut2);

        UpdateResult updateResult = Mockito.mock(UpdateResult.class);
        UpdateResult updateResult2 = Mockito.mock(UpdateResult.class);
        Mockito.when(hpanInitiativesRepositoryMock.setInitiative(Mockito.anyString(), Mockito.eq(onboardedInitiativeOut1))).thenReturn(Mono.just(updateResult));
        Mockito.when(hpanInitiativesRepositoryMock.setInitiative(Mockito.anyString(), Mockito.eq(onboardedInitiativeOut2))).thenReturn(Mono.just(updateResult2));

        Acknowledgment notValidJsonAck = Mockito.mock(Acknowledgment.class);

        // When
        hpanInitiativeMediatorService.execute(Flux
               .fromIterable(List.of(MessageBuilder.withPayload(TestUtils.jsonSerializer(hpanInitiativeBulkDTOValidJson)).copyHeaders(Map.of(KafkaHeaders.ACKNOWLEDGMENT, hpanInitiativeBulkDTOValidJsonAck)).build(),
                        MessageBuilder.withPayload(TestUtils.jsonSerializer(hpanInitiativeBulkDTONotHpanList)).copyHeaders(Map.of(KafkaHeaders.ACKNOWLEDGMENT, hpanInitiativeBulkDTONotHpanListAck)).build(),
                        MessageBuilder.withPayload(TestUtils.jsonSerializer(hpanInitiativeBulkDTONotDate)).copyHeaders(Map.of(KafkaHeaders.ACKNOWLEDGMENT, hpanInitiativeBulkDTONotDateAck)).build(),
                        MessageBuilder.withPayload("NOT VALID JSON").copyHeaders(Map.of(KafkaHeaders.ACKNOWLEDGMENT, notValidJsonAck)).build()
                )));
        // Then
        Mockito.verify(hpanInitiativesRepositoryMock,Mockito.times(2)).findById(Mockito.anyString());
        Mockito.verify(hpanInitiativesServiceMock,Mockito.times(2)).evaluate(Mockito.any(HpanUpdateEvaluateDTO.class),Mockito.any(HpanInitiatives.class));
        Mockito.verify(hpanInitiativesRepositoryMock, Mockito.times(2)).setInitiative(Mockito.anyString(), Mockito.any());
        Mockito.verify(errorNotifierServiceMock,Mockito.times(2)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(Throwable.class));
        Mockito.verify(errorNotifierServiceMock, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(NullPointerException.class));
        Mockito.verify(errorNotifierServiceMock, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));
    }

    @Test
    void otherApplicationRetryTest(){
        // Given
        HpanInitiativeBulkDTO initiative1 = HpanInitiativeBulkDTOFaker.mockInstanceBuilder(1)
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now()).build();
        HpanInitiativeBulkDTO initiative2 = HpanInitiativeBulkDTOFaker.mockInstanceBuilder(2)
                .operationType(HpanInitiativeConstants.ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now()).build();


        Flux<Message<String>> msgs = Flux.just(initiative1, initiative2)
                .map(TestUtils::jsonSerializer)
                .map(MessageBuilder::withPayload)
                .doOnNext(m->m.setHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, "otherAppName".getBytes(StandardCharsets.UTF_8)))
                .map(MessageBuilder::build);

        // When
        hpanInitiativeMediatorService.execute(msgs);

        // Then
        Mockito.verifyNoInteractions(hpanInitiativesRepositoryMock, hpanInitiativesServiceMock, errorNotifierServiceMock, hpanUpdateEvaluateDTO2HpanInitiativeMapperMock, hpanUpdateBulk2SingleMapperMock);
    }
}