package it.gov.pagopa.reward.service.lookup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.connector.repository.secondary.HpanInitiativesRepository;
import it.gov.pagopa.reward.connector.repository.secondary.OnboardingFamiliesRepository;
import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.*;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.lookup.HpanList2HpanUpdateOutcomeDTOMapper;
import it.gov.pagopa.reward.dto.mapper.lookup.HpanUpdateBulk2SingleMapper;
import it.gov.pagopa.reward.dto.mapper.lookup.HpanUpdateEvaluateDTO2HpanInitiativeMapper;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.OnboardingFamilies;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.HpanInitiativeBulkDTOFaker;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.fakers.OnboardingFamiliesFaker;
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
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class HpanInitiativeMediatorServiceImplTest {

    @Mock
    private HpanInitiativesRepository hpanInitiativesRepositoryMock;
    @Mock
    private UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock
    private HpanInitiativesService hpanInitiativesServiceMock;
    @Mock
    private RewardErrorNotifierService rewardErrorNotifierServiceMock;
    @Mock
    private HpanUpdateEvaluateDTO2HpanInitiativeMapper hpanUpdateEvaluateDTO2HpanInitiativeMapperMock;

    @Mock
    private HpanUpdateNotifierService hpanUpdateNotifierServiceMock;
    @Mock
    private HpanUpdateBulk2SingleMapper hpanUpdateBulk2SingleMapperMock;

    @Mock
    private HpanList2HpanUpdateOutcomeDTOMapper hpanList2HpanUpdateOutcomeDTOMapperMock;

    @Mock private RewardContextHolderService rewardContextHolderServiceMock;
    @Mock private OnboardingFamiliesRepository onboardingFamiliesRepositoryMock;

    private HpanInitiativeMediatorServiceImpl hpanInitiativeMediatorService;
    @BeforeEach
    void setUp(){
        hpanInitiativeMediatorService = new HpanInitiativeMediatorServiceImpl(
                "appName",
                0L,
                hpanInitiativesRepositoryMock,
                userInitiativeCountersRepositoryMock,
                hpanInitiativesServiceMock,
                hpanUpdateNotifierServiceMock,
                TestUtils.objectMapper,
                rewardErrorNotifierServiceMock,
                hpanUpdateEvaluateDTO2HpanInitiativeMapperMock,
                hpanUpdateBulk2SingleMapperMock,
                hpanList2HpanUpdateOutcomeDTOMapperMock,
                rewardContextHolderServiceMock,
                onboardingFamiliesRepositoryMock);
    }
    @Test
    void execute(){
        // Given
        //region input test
        String hpanValid = "HPAN_VALID";
        Acknowledgment hpanInitiativeBulkDTOValidJsonAck = Mockito.mock(Acknowledgment.class);

        PaymentMethodInfoDTO infoHpan = getPaymentMethodInfoDTO(hpanValid);
        HpanInitiativeBulkDTO hpanInitiativeBulkDTOValidJson = HpanInitiativeBulkDTO.builder()
                .userId("USERID_HPAN_VALID")
                .infoList(List.of(infoHpan))
                .initiativeId("INITIATIVEID_HPAN_VALID")
                .operationType(HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT)
                .operationDate(LocalDateTime.now().plusDays(10L))
                .channel("ANOTHER_CHANNEL").build();

        HpanUpdateEvaluateDTO hpanUpdateValidHpan = getHpanUpdateEvaluateDTO(hpanInitiativeBulkDTOValidJson, infoHpan);
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

        HpanUpdateEvaluateDTO hpanUpdateNotDate = getHpanUpdateEvaluateDTO(hpanInitiativeBulkDTONotDate, infoHpanNotDate);
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
        ActiveTimeInterval activeTimeInterval1 = ActiveTimeInterval.builder()
                .startInterval(LocalDateTime.now().minusMonths(10L))
                .endInterval(LocalDateTime.now().minusMonths(8L)).build();

        ActiveTimeInterval activeTimeInterval2 = ActiveTimeInterval.builder()
                .startInterval(LocalDateTime.now().minusMonths(5L))
                .endInterval(LocalDateTime.now().minusMonths(3L)).build();

        OnboardedInitiative onboardedInitiativeOut1 = OnboardedInitiative.builder().initiativeId("INITIATIVEID_HPAN_VALID")
                .activeTimeIntervals(List.of(activeTimeInterval1, activeTimeInterval2))
                .build();

        OnboardedInitiative onboardedInitiativeOut2 = OnboardedInitiative.builder().initiativeId("INITIATIVEID_HPAN_NOT_DATE")
                .activeTimeIntervals(List.of(activeTimeInterval1, activeTimeInterval2))
                .build();
        OnboardedInitiative onboardedInitiativeOut3 = OnboardedInitiative.builder().initiativeId("INITIATIVEID_HPAN_ERROR_PUBLISHED")
                .activeTimeIntervals(List.of(activeTimeInterval1, activeTimeInterval2))
                .build();

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
        Mockito.verify(rewardErrorNotifierServiceMock,Mockito.times(2)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(Throwable.class));
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(NullPointerException.class));
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));
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
        Mockito.verifyNoInteractions(hpanInitiativesRepositoryMock, hpanInitiativesServiceMock, rewardErrorNotifierServiceMock, hpanUpdateEvaluateDTO2HpanInitiativeMapperMock, hpanUpdateBulk2SingleMapperMock);
    }

    @Test
    void executeForInitiativeTypeNF(){
        // Given
        String hpan = "HPAN_VALID";
        String userId = "USERID_HPAN_VALID";
        String initiativeId = "INITIATIVEID_HPAN_VALID";
        Acknowledgment hpanInitiativeBulkDTOValidJsonAck = Mockito.mock(Acknowledgment.class);

        PaymentMethodInfoDTO infoHpan = getPaymentMethodInfoDTO(hpan);
        HpanInitiativeBulkDTO hpanInitiativeBulkDTOValidJson = getHpanInitiativeBulkDTO(userId, infoHpan, initiativeId, HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT, "ANOTHER_CHANNEL");

        HpanUpdateEvaluateDTO hpanUpdateValidHpan = getHpanUpdateEvaluateDTO(hpanInitiativeBulkDTOValidJson, infoHpan);
        Mockito.when(hpanUpdateBulk2SingleMapperMock.apply(Mockito.eq(hpanInitiativeBulkDTOValidJson),Mockito.eq(infoHpan),Mockito.any())).thenReturn(hpanUpdateValidHpan);

        //findAndModify
        HpanInitiatives hpanInitiatives1 = HpanInitiativesFaker.mockInstance(1);
        hpanInitiatives1.setHpan(hpan);
        Mockito.when(hpanInitiativesRepositoryMock.findById(hpan)).thenReturn(Mono.just(hpanInitiatives1));

        ActiveTimeInterval activeTimeInterval1 = ActiveTimeInterval.builder()
                .startInterval(LocalDateTime.now().minusMonths(10L))
                .endInterval(LocalDateTime.now().minusMonths(8L)).build();

        OnboardedInitiative onboardedInitiativeOut1 = OnboardedInitiative.builder().initiativeId(initiativeId)
                .activeTimeIntervals(List.of(activeTimeInterval1))
                .familyId("FAMILYID")
                .build();

        Mockito.when(hpanInitiativesServiceMock.evaluate(hpanUpdateValidHpan,hpanInitiatives1))
                .thenReturn(onboardedInitiativeOut1);

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF).build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId))
                .thenReturn(Mono.just(initiativeConfig));

        OnboardingFamilies of1 = OnboardingFamiliesFaker.mockInstance(1);
        of1.setFamilyId("FAMILYID0");
        of1.setMemberIds(Set.of(userId));
        of1.setCreateDate(LocalDateTime.now().minusMonths(10L));
        OnboardingFamilies of2 = OnboardingFamiliesFaker.mockInstance(1);
        of2.setFamilyId("FAMILYID");
        of2.setMemberIds(Set.of(userId));
        of2.setCreateDate(LocalDateTime.now().minusMonths(5L));
        Mockito.when(onboardingFamiliesRepositoryMock.findByMemberIdsInAndInitiativeId(userId, initiativeId))
                .thenReturn(Flux.just(of1, of2));

        UpdateResult updateResult = Mockito.mock(UpdateResult.class);
        Mockito.when(userInitiativeCountersRepositoryMock.createIfNotExists("FAMILYID", InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, initiativeId))
                .thenReturn(Mono.just(updateResult));

        UpdateResult updateResult2 = Mockito.mock(UpdateResult.class);
        Mockito.when(hpanInitiativesRepositoryMock.setInitiative(Mockito.any(),Mockito.any()))
                .thenReturn(Mono.just(updateResult2));

        List<String> jsonValidList = hpanInitiativeBulkDTOValidJson.getInfoList().stream().map(PaymentMethodInfoDTO::getHpan).toList();
        HpanUpdateOutcomeDTO jsonValidOutcome = HpanUpdateOutcomeDTO.builder()
                .userId(hpanInitiativeBulkDTOValidJson.getUserId())
                .initiativeId(hpanInitiativeBulkDTOValidJson.getInitiativeId())
                .hpanList(jsonValidList)
                .rejectedHpanList(new ArrayList<>())
                .build();
        Mockito.when(hpanList2HpanUpdateOutcomeDTOMapperMock.apply(Mockito.eq(jsonValidList),Mockito.eq(hpanInitiativeBulkDTOValidJson),Mockito.any())).thenReturn(jsonValidOutcome);
        Mockito.when(hpanUpdateNotifierServiceMock.notify(jsonValidOutcome)).thenReturn(true);

        // When
        hpanInitiativeMediatorService.execute(Flux
                .fromIterable(List.of(buildMessage(TestUtils.jsonSerializer(hpanInitiativeBulkDTOValidJson), hpanInitiativeBulkDTOValidJsonAck))));
        // Then
        Mockito.verify(hpanInitiativesRepositoryMock,Mockito.times(1)).findById(Mockito.anyString());
        Mockito.verify(hpanInitiativesServiceMock,Mockito.times(1)).evaluate(Mockito.any(HpanUpdateEvaluateDTO.class),Mockito.any(HpanInitiatives.class));
        Mockito.verify(hpanInitiativesRepositoryMock, Mockito.times(1)).setInitiative(Mockito.anyString(), Mockito.any());
        Mockito.verify(rewardErrorNotifierServiceMock,Mockito.never()).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(Throwable.class));
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.never()).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(NullPointerException.class));
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.never()).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));

    }

    @Test
    void executeForInitiativeTypeNFWithNewInitaitve(){
        // Given
        String hpan = "HPAN_VALID";
        String userId = "USERID_HPAN_VALID";
        String initiativeId = "INITIATIVEID_HPAN_VALID";
        Acknowledgment hpanInitiativeBulkDTOValidJsonAck = Mockito.mock(Acknowledgment.class);

        PaymentMethodInfoDTO infoHpan = getPaymentMethodInfoDTO(hpan);
        HpanInitiativeBulkDTO hpanInitiativeBulkDTOValidJson = getHpanInitiativeBulkDTO(userId, infoHpan, initiativeId, HpanInitiativeConstants.OPERATION_ADD_INSTRUMENT,"ANOTHER_CHANNEL");

        HpanUpdateEvaluateDTO hpanUpdateValidHpan = getHpanUpdateEvaluateDTO(hpanInitiativeBulkDTOValidJson, infoHpan);
        Mockito.when(hpanUpdateBulk2SingleMapperMock.apply(Mockito.eq(hpanInitiativeBulkDTOValidJson),Mockito.eq(infoHpan),Mockito.any())).thenReturn(hpanUpdateValidHpan);

        Mockito.when(hpanInitiativesRepositoryMock.findById(hpan)).thenReturn(Mono.empty());

        HpanInitiatives hpanInitiatives1 = HpanInitiativesFaker.mockInstance(1);
        hpanInitiatives1.setHpan(hpan);
        Mockito.when(hpanUpdateEvaluateDTO2HpanInitiativeMapperMock.apply(hpanUpdateValidHpan))
                .thenReturn(hpanInitiatives1);
        Mockito.when(hpanInitiativesRepositoryMock.createIfNotExist(hpanInitiatives1)).thenReturn(Mono.just(Mockito.mock(UpdateResult.class)));

        ActiveTimeInterval activeTimeInterval1 = ActiveTimeInterval.builder()
                .startInterval(LocalDateTime.now().minusMonths(10L))
                .endInterval(LocalDateTime.now().minusMonths(8L)).build();

        OnboardedInitiative onboardedInitiativeOut1 = OnboardedInitiative.builder().initiativeId(initiativeId)
                .activeTimeIntervals(List.of(activeTimeInterval1))
                .familyId("FAMILYID")
                .build();

        Mockito.when(hpanInitiativesServiceMock.evaluate(hpanUpdateValidHpan,hpanInitiatives1))
                .thenReturn(onboardedInitiativeOut1);

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF).build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId))
                .thenReturn(Mono.just(initiativeConfig));

        OnboardingFamilies of1 = OnboardingFamiliesFaker.mockInstance(1);
        of1.setFamilyId("FAMILYID0");
        of1.setMemberIds(Set.of(userId));
        of1.setCreateDate(LocalDateTime.now().minusMonths(10L));
        OnboardingFamilies of2 = OnboardingFamiliesFaker.mockInstance(1);
        of2.setFamilyId("FAMILYID");
        of2.setMemberIds(Set.of(userId));
        of2.setCreateDate(LocalDateTime.now().minusMonths(5L));
        Mockito.when(onboardingFamiliesRepositoryMock.findByMemberIdsInAndInitiativeId(userId, initiativeId))
                .thenReturn(Flux.just(of1, of2));

        UpdateResult updateResult = Mockito.mock(UpdateResult.class);
        Mockito.when(userInitiativeCountersRepositoryMock.createIfNotExists("FAMILYID", InitiativeGeneralDTO.BeneficiaryTypeEnum.NF, initiativeId))
                .thenReturn(Mono.just(updateResult));

        UpdateResult updateResult2 = Mockito.mock(UpdateResult.class);
        Mockito.when(hpanInitiativesRepositoryMock.setInitiative(Mockito.any(),Mockito.any()))
                .thenReturn(Mono.just(updateResult2));

        List<String> jsonValidList = hpanInitiativeBulkDTOValidJson.getInfoList().stream().map(PaymentMethodInfoDTO::getHpan).toList();
        HpanUpdateOutcomeDTO jsonValidOutcome = HpanUpdateOutcomeDTO.builder()
                .userId(hpanInitiativeBulkDTOValidJson.getUserId())
                .initiativeId(hpanInitiativeBulkDTOValidJson.getInitiativeId())
                .hpanList(jsonValidList)
                .rejectedHpanList(new ArrayList<>())
                .build();
        Mockito.when(hpanList2HpanUpdateOutcomeDTOMapperMock.apply(Mockito.eq(jsonValidList),Mockito.eq(hpanInitiativeBulkDTOValidJson),Mockito.any())).thenReturn(jsonValidOutcome);
        Mockito.when(hpanUpdateNotifierServiceMock.notify(jsonValidOutcome)).thenReturn(true);

        // When
        hpanInitiativeMediatorService.execute(Flux
                .fromIterable(List.of(buildMessage(TestUtils.jsonSerializer(hpanInitiativeBulkDTOValidJson), hpanInitiativeBulkDTOValidJsonAck))));
        // Then
        Mockito.verify(hpanInitiativesRepositoryMock,Mockito.times(1)).findById(Mockito.anyString());
        Mockito.verify(hpanInitiativesServiceMock,Mockito.times(1)).evaluate(Mockito.any(HpanUpdateEvaluateDTO.class),Mockito.any(HpanInitiatives.class));
        Mockito.verify(hpanInitiativesRepositoryMock, Mockito.times(1)).setInitiative(Mockito.anyString(), Mockito.any());
        Mockito.verify(rewardErrorNotifierServiceMock,Mockito.never()).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(Throwable.class));
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.never()).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(NullPointerException.class));
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.never()).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));
    }

    @Test
    void executeForInitiativeTypeNFHpanNotInDB(){
        // Given
        String hpan = "HPAN_VALID";
        String userId = "USERID_HPAN_VALID";
        String initiativeId = "INITIATIVEID_HPAN_VALID";
        Acknowledgment hpanInitiativeBulkDTOValidJsonAck = Mockito.mock(Acknowledgment.class);

        PaymentMethodInfoDTO infoHpan = getPaymentMethodInfoDTO(hpan);
        HpanInitiativeBulkDTO hpanInitiativeBulkDTOValidJson = getHpanInitiativeBulkDTO(userId, infoHpan, initiativeId, HpanInitiativeConstants.OPERATION_DELETE_INSTRUMENT,"ANOTHER_CHANNEL");

        HpanUpdateEvaluateDTO hpanUpdateValidHpan = getHpanUpdateEvaluateDTO(hpanInitiativeBulkDTOValidJson, infoHpan);
        Mockito.when(hpanUpdateBulk2SingleMapperMock.apply(Mockito.eq(hpanInitiativeBulkDTOValidJson),Mockito.eq(infoHpan),Mockito.any())).thenReturn(hpanUpdateValidHpan);

        Mockito.when(hpanInitiativesRepositoryMock.findById(hpan)).thenReturn(Mono.empty());

        List<String> jsonValidList = hpanInitiativeBulkDTOValidJson.getInfoList().stream().map(PaymentMethodInfoDTO::getHpan).toList();
        HpanUpdateOutcomeDTO jsonValidOutcome = HpanUpdateOutcomeDTO.builder()
                .userId(hpanInitiativeBulkDTOValidJson.getUserId())
                .initiativeId(hpanInitiativeBulkDTOValidJson.getInitiativeId())
                .hpanList(jsonValidList)
                .rejectedHpanList(new ArrayList<>())
                .build();
        Mockito.when(hpanList2HpanUpdateOutcomeDTOMapperMock.apply(Mockito.eq(jsonValidList),Mockito.eq(hpanInitiativeBulkDTOValidJson),Mockito.any())).thenReturn(jsonValidOutcome);
        Mockito.when(hpanUpdateNotifierServiceMock.notify(jsonValidOutcome)).thenReturn(true);

        // When
        hpanInitiativeMediatorService.execute(Flux
                .fromIterable(List.of(buildMessage(TestUtils.jsonSerializer(hpanInitiativeBulkDTOValidJson), hpanInitiativeBulkDTOValidJsonAck))));
        // Then
        Mockito.verify(hpanInitiativesRepositoryMock,Mockito.times(1)).findById(Mockito.anyString());
        Mockito.verify(hpanInitiativesServiceMock,Mockito.never()).evaluate(Mockito.any(HpanUpdateEvaluateDTO.class),Mockito.any(HpanInitiatives.class));
        Mockito.verify(hpanInitiativesRepositoryMock, Mockito.never()).setInitiative(Mockito.anyString(), Mockito.any());
        Mockito.verify(rewardErrorNotifierServiceMock,Mockito.times(1)).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(Throwable.class));
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.never()).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(NullPointerException.class));
        Mockito.verify(rewardErrorNotifierServiceMock, Mockito.never()).notifyHpanUpdateEvaluation(Mockito.any(Message.class),Mockito.anyString(),Mockito.anyBoolean(), Mockito.any(JsonProcessingException.class));

    }

    private static HpanInitiativeBulkDTO getHpanInitiativeBulkDTO(String userId, PaymentMethodInfoDTO infoHpan, String initiativeId, String operationType, String channel) {
        return HpanInitiativeBulkDTO.builder()
                .userId(userId)
                .infoList(List.of(infoHpan))
                .initiativeId(initiativeId)
                .operationType(operationType)
                .operationDate(LocalDateTime.now().plusDays(10L))
                .channel(channel).build();
    }

    private static PaymentMethodInfoDTO getPaymentMethodInfoDTO(String hpan) {
        return PaymentMethodInfoDTO.builder()
                .hpan(hpan)
                .maskedPan("MASKEDPAN_HPAN_VALID")
                .brandLogo("BRANDLOGO_HPAN_VALID").build();
    }
    private static HpanUpdateEvaluateDTO getHpanUpdateEvaluateDTO(HpanInitiativeBulkDTO hpanInitiativeBulkDTOValidJson, PaymentMethodInfoDTO infoHpan) {
        return HpanUpdateEvaluateDTO.builder()
                .userId(hpanInitiativeBulkDTOValidJson.getUserId())
                .initiativeId(hpanInitiativeBulkDTOValidJson.getInitiativeId())
                .hpan(infoHpan.getHpan())
                .maskedPan(infoHpan.getMaskedPan())
                .brandLogo(infoHpan.getBrandLogo())
                .operationType(hpanInitiativeBulkDTOValidJson.getOperationType())
                .evaluationDate(LocalDateTime.now())
                .build();
    }
}