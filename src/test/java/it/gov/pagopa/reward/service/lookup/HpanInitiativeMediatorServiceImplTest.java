package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.reactive.kafka.utils.KafkaConstants;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.dto.HpanUpdateEvaluateDTO;
import it.gov.pagopa.reward.dto.HpanUpdateOutcomeDTO;
import it.gov.pagopa.reward.dto.PaymentMethodInfoDTO;
import it.gov.pagopa.reward.dto.mapper.lookup.HpanList2HpanUpdateOutcomeDTOMapper;
import it.gov.pagopa.reward.dto.mapper.lookup.HpanUpdateBulk2SingleMapper;
import it.gov.pagopa.reward.dto.mapper.lookup.HpanUpdateEvaluateDTO2HpanInitiativeMapper;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeBulkDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.utils.HpanInitiativeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@ExtendWith(MockitoExtension.class)
class HpanInitiativeMediatorServiceImplTest {

    @Mock
    private HpanInitiativesRepository hpanInitiativesRepositoryMock;
    @Mock
    private HpanInitiativesService hpanInitiativesServiceMock;
    @Mock
    private ErrorNotifierService errorNotifierServiceMock;
    @Mock
    private HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapperMock;

    @Mock
    private HpanUpdateNotifierService hpanUpdateNotifierServiceMock;
    @Mock
    private HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapperMock;

    @Mock
    private HpanList2HpanUpdateOutcomeDTOMapper hpanList2HpanUpdateOutcomeDTOMapperMock;

    private HpanInitiativeMediatorService hpanInitiativeMediatorService;
    @BeforeEach
    void setUp(){
        hpanInitiativeMediatorService = new HpanInitiativeMediatorServiceImpl("appName",0L, hpanInitiativesRepositoryMock, hpanInitiativesServiceMock, hpanUpdateNotifierServiceMock, TestUtils.objectMapper, errorNotifierServiceMock, hpanUpdateEvaluateDTO2HpanInitiativeMapperMock, hpanUpdateBulk2SingleMapperMock, hpanList2HpanUpdateOutcomeDTOMapperMock);
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
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now().plusDays(10L))
                .channel("ANOTHER_CHANNEL").build();

        HpanUpdateEvaluateDTO hpanUpdateValidHpan = HpanUpdateEvaluateDTO.builder()
                .userId(hpanInitiativeBulkDTOValidJson.getUserId())
                .initiativeId(hpanInitiativeBulkDTOValidJson.getInitiativeId())
                .hpan(infoHpan.getHpan())
                .maskedPan(infoHpan.getMaskedPan())
                .brandLogo(infoHpan.getBrandLogo())
                .operationType(hpanInitiativeBulkDTOValidJson.getOperationType())
                .evaluationDate(LocalDateTime.now())
                .build();
        Mockito.when(hpanUpdateBulk2SingleMapperMock.apply(Mockito.eq(hpanInitiativeBulkDTOValidJson),Mockito.eq(infoHpan),Mockito.any())).thenReturn(hpanUpdateValidHpan);

        HpanInitiatives hpanInitiatives1 = HpanInitiativesFaker.mockInstance(1);
        hpanInitiatives1.setHpan(hpanValid);
        Mockito.when(hpanInitiativesRepositoryMock.findById(hpanValid)).thenReturn(Mono.just(hpanInitiatives1));


        // Message without hpan
        Acknowledgment hpanInitiativeBulkDTONotHpanListAck = Mockito.mock(Acknowledgment.class);
        HpanInitiativeBulkDTO hpanInitiativeBulkDTONotHpanList = HpanInitiativeBulkDTO.builder()
                .userId("USERID")
                .initiativeId("INITIATIVEID")
                .operationType(HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT)
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
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .channel(HpanInitiativeConstants.CHANEL_PAYMENT_MANAGER).build();

        HpanUpdateEvaluateDTO hpanUpdateNotDate = HpanUpdateEvaluateDTO.builder()
                .userId(hpanInitiativeBulkDTONotDate.getUserId())
                .initiativeId(hpanInitiativeBulkDTONotDate.getInitiativeId())
                .hpan(infoHpanNotDate.getHpan())
                .maskedPan(infoHpanNotDate.getMaskedPan())
                .brandLogo(infoHpanNotDate.getBrandLogo())
                .operationType(hpanInitiativeBulkDTONotDate.getOperationType())
                .evaluationDate(LocalDateTime.now())
                .build();
        Mockito.when(hpanUpdateBulk2SingleMapperMock.apply(Mockito.eq(hpanInitiativeBulkDTONotDate),Mockito.eq(infoHpanNotDate), Mockito.any())).thenReturn(hpanUpdateNotDate);

        HpanInitiatives hpanInitiatives2 = HpanInitiativesFaker.mockInstance(2);
        hpanInitiatives2.setHpan(hpanNotDate);
        Mockito.when(hpanInitiativesRepositoryMock.findById(hpanNotDate)).thenReturn(Mono.just(hpanInitiatives2));

        //error published update
        String hpanErrorPublishedUpdate = "HPAN_ERROR_PUBLISHED_UPDATE";
        Acknowledgment hpanErrorPublishedUpdateJsonAck = Mockito.mock(Acknowledgment.class);

        PaymentMethodInfoDTO infoHpanErrorPublishedUpdate = PaymentMethodInfoDTO.builder()
                .hpan(hpanErrorPublishedUpdate)
                .maskedPan("MASKEDPAN_ERROR_PUBLISHED_UPDATE")
                .brandLogo("BRANDLOGO_ERROR_PUBLISHED_UPDATE").build();
        HpanInitiativeBulkDTO hpanInitiativeBulkDTErrorPublishedUpdate = HpanInitiativeBulkDTO.builder()
                .userId("USERID_ERROR_PUBLISHED_UPDATE")
                .infoList(List.of(infoHpanErrorPublishedUpdate))
                .initiativeId("INITIATIVEID_ERROR_PUBLISHED_UPDATE")
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now())
                .channel("ANOTHER_CHANNEL").build();

        HpanUpdateEvaluateDTO hpanUpdateErrorPublished = HpanUpdateEvaluateDTO.builder()
                .userId(hpanInitiativeBulkDTErrorPublishedUpdate.getUserId())
                .initiativeId(hpanInitiativeBulkDTErrorPublishedUpdate.getInitiativeId())
                .hpan(infoHpanErrorPublishedUpdate.getHpan())
                .maskedPan(infoHpanErrorPublishedUpdate.getMaskedPan())
                .brandLogo(infoHpanErrorPublishedUpdate.getBrandLogo())
                .operationType(hpanInitiativeBulkDTErrorPublishedUpdate.getOperationType())
                .evaluationDate(LocalDateTime.now().with(LocalDateTime.MIN))
                .build();
        Mockito.when(hpanUpdateBulk2SingleMapperMock.apply(Mockito.eq(hpanInitiativeBulkDTErrorPublishedUpdate),Mockito.eq(infoHpanErrorPublishedUpdate),Mockito.any())).thenReturn(hpanUpdateErrorPublished);

        HpanInitiatives hpanInitiatives3 = HpanInitiativesFaker.mockInstance(3);
        hpanInitiatives3.setHpan(hpanErrorPublishedUpdate);
        Mockito.when(hpanInitiativesRepositoryMock.findById(hpanErrorPublishedUpdate)).thenReturn(Mono.just(hpanInitiatives3));


        //endregion
        OnboardedInitiative onboardedInitiativeOut1 = OnboardedInitiative.builder().initiativeId("INITIATIVEID_HPAN_VALID").build();

        OnboardedInitiative onboardedInitiativeOut2 = OnboardedInitiative.builder().initiativeId("INITIATIVEID_HPAN_NOT_DATE").build();
        OnboardedInitiative onboardedInitiativeOut3 = OnboardedInitiative.builder().initiativeId("INITIATIVEID_HPAN_ERROR_PUBLISHED").build();

        Mockito.when(hpanInitiativesServiceMock.evaluate(hpanUpdateValidHpan,hpanInitiatives1))
                .thenReturn(onboardedInitiativeOut1);
        Mockito.when(hpanInitiativesServiceMock.evaluate(hpanUpdateNotDate,hpanInitiatives2))
                .thenReturn(onboardedInitiativeOut2);
        Mockito.when(hpanInitiativesServiceMock.evaluate(hpanUpdateErrorPublished,hpanInitiatives3))
                .thenReturn(onboardedInitiativeOut3);

        UpdateResult updateResult = Mockito.mock(UpdateResult.class);
        UpdateResult updateResult2 = Mockito.mock(UpdateResult.class);
        UpdateResult updateResult3 = Mockito.mock(UpdateResult.class);
        Mockito.when(hpanInitiativesRepositoryMock.setInitiative(Mockito.anyString(), Mockito.eq(onboardedInitiativeOut1))).thenReturn(Mono.just(updateResult));
        Mockito.when(hpanInitiativesRepositoryMock.setInitiative(Mockito.anyString(), Mockito.eq(onboardedInitiativeOut2))).thenReturn(Mono.just(updateResult2));
        Mockito.when(hpanInitiativesRepositoryMock.setInitiative(Mockito.anyString(), Mockito.eq(onboardedInitiativeOut3))).thenReturn(Mono.just(updateResult3));

        List<String> jsonValidList = hpanInitiativeBulkDTOValidJson.getInfoList().stream().map(PaymentMethodInfoDTO::getHpan).toList();
        HpanUpdateOutcomeDTO jsonValidOutcome = HpanUpdateOutcomeDTO.builder()
                .userId(hpanInitiativeBulkDTOValidJson.getUserId())
                .initiativeId(hpanInitiativeBulkDTOValidJson.getInitiativeId())
                .hpanList(jsonValidList)
                .rejectedHpanList(new ArrayList<>())
                .build();
        Mockito.when(hpanList2HpanUpdateOutcomeDTOMapperMock.apply(Mockito.eq(jsonValidList),Mockito.eq(hpanInitiativeBulkDTOValidJson),Mockito.any())).thenReturn(jsonValidOutcome);
        Mockito.when(hpanUpdateNotifierServiceMock.notify(jsonValidOutcome)).thenReturn(true);
        Acknowledgment notValidJsonAck = Mockito.mock(Acknowledgment.class);

        List<String> errorPublishedHpanList = hpanInitiativeBulkDTErrorPublishedUpdate.getInfoList().stream().map(PaymentMethodInfoDTO::getHpan).toList();
        HpanUpdateOutcomeDTO errorPublishedOutcome = HpanUpdateOutcomeDTO.builder()
                .userId(hpanInitiativeBulkDTErrorPublishedUpdate.getUserId())
                .initiativeId(hpanInitiativeBulkDTErrorPublishedUpdate.getInitiativeId())
                .hpanList(errorPublishedHpanList)
                .rejectedHpanList(new ArrayList<>())
                .build();
        Mockito.when(hpanList2HpanUpdateOutcomeDTOMapperMock.apply(Mockito.eq(errorPublishedHpanList),Mockito.eq(hpanInitiativeBulkDTErrorPublishedUpdate),Mockito.any())).thenReturn(errorPublishedOutcome);
        Mockito.when(hpanUpdateNotifierServiceMock.notify(errorPublishedOutcome)).thenReturn(false);

        // When
        hpanInitiativeMediatorService.execute(Flux
               .fromIterable(List.of(buildMessage(TestUtils.jsonSerializer(hpanInitiativeBulkDTOValidJson), hpanInitiativeBulkDTOValidJsonAck),
                       buildMessage(TestUtils.jsonSerializer(hpanInitiativeBulkDTONotHpanList), hpanInitiativeBulkDTONotHpanListAck),
                       buildMessage(TestUtils.jsonSerializer(hpanInitiativeBulkDTONotDate), hpanInitiativeBulkDTONotDateAck),
                       buildMessage("NOT VALID JSON", notValidJsonAck),
                       buildMessage(TestUtils.jsonSerializer(hpanInitiativeBulkDTErrorPublishedUpdate), hpanErrorPublishedUpdateJsonAck)
                )));
        // Then
        Mockito.verify(hpanInitiativesRepositoryMock,Mockito.times(3)).findById(Mockito.anyString());
        Mockito.verify(hpanInitiativesServiceMock,Mockito.times(3)).evaluate(Mockito.any(HpanUpdateEvaluateDTO.class),Mockito.any(HpanInitiatives.class));
        Mockito.verify(hpanInitiativesRepositoryMock, Mockito.times(3)).setInitiative(Mockito.anyString(), Mockito.any());
        Mockito.verify(errorNotifierServiceMock,Mockito.times(2)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(Throwable.class));
        Mockito.verify(errorNotifierServiceMock, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(NullPointerException.class));
        Mockito.verify(errorNotifierServiceMock, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));
    }

    private static Message<String> buildMessage(String hpanInitiativeBulkDTONotHpanList, Acknowledgment hpanInitiativeBulkDTONotHpanListAck) {
        return MessageBuilder
                .withPayload(hpanInitiativeBulkDTONotHpanList)
                .setHeader(KafkaHeaders.ACKNOWLEDGMENT, hpanInitiativeBulkDTONotHpanListAck)
                .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                .setHeader(KafkaHeaders.OFFSET, 0L)
                .build();
    }

    @Test
    void otherApplicationRetryTest(){
        // Given
        HpanInitiativeBulkDTO initiative1 = HpanInitiativeBulkDTOFaker.mockInstanceBuilder(1)
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now()).build();
        HpanInitiativeBulkDTO initiative2 = HpanInitiativeBulkDTOFaker.mockInstanceBuilder(2)
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now()).build();


        Flux<Message<String>> msgs = Flux.just(initiative1, initiative2)
                .map(TestUtils::jsonSerializer)
                .map(MessageBuilder::withPayload)
                .doOnNext(m->m.setHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "otherAppName".getBytes(StandardCharsets.UTF_8)))
                .map(MessageBuilder::build);

        // When
        hpanInitiativeMediatorService.execute(msgs);

        // Then
        Mockito.verifyNoInteractions(hpanInitiativesRepositoryMock, hpanInitiativesServiceMock, errorNotifierServiceMock, hpanUpdateEvaluateDTO2HpanInitiativeMapperMock, hpanUpdateBulk2SingleMapperMock);
    }
}