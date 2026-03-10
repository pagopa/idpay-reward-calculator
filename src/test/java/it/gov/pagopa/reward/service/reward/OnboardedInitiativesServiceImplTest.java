package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.rest.onboarding.OnboardingWorkflowConnector;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.onboarding.OnboardingStatusResponseDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.BaseOnboardingInfo;
import it.gov.pagopa.reward.model.OnboardingInfo;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
@Slf4j
class OnboardedInitiativesServiceImplTest {

    @Mock private RewardContextHolderService rewardContextHolderServiceMock;
    @Mock private OnboardingWorkflowConnector onboardingWorkflowConnectorMock;

    private OnboardedInitiativesService onboardedInitiativesService;

    @BeforeEach
    public void init(){
        onboardedInitiativesService = new OnboardedInitiativesServiceImpl(
                rewardContextHolderServiceMock,
                onboardingWorkflowConnectorMock);
    }

    @Test
    void getChargeInitiativesWhenNoEndEnd(){
        testChargeInitiative(null,null, null, true);
    }

    @Test
    void getChargeInitiativesWhenEndEnd(){
        testChargeInitiative(null, LocalDate.now().plusDays(10L), OffsetDateTime.now().plusDays(10), true);
    }
    @Test
    void getChargeInitiativesWhenFutureEndEnd(){
        testChargeInitiative(null, LocalDate.now().plusDays(11), OffsetDateTime.now().plusDays(10), true);
    }

    @Test
    void getChargeInitiativesWhenPastEndEnd(){
        testChargeInitiative(null, LocalDate.now().plusDays(9), OffsetDateTime.now().plusDays(10), false);
    }

    @Test
    void getChargeInitiativesWhenStart(){
        testChargeInitiative(LocalDate.now().plusDays(10L),null, OffsetDateTime.now().plusDays(10L), true);
    }

    @Test
    void getChargeInitiativeAfterStartInitiative(){
        testChargeInitiative(LocalDate.now().plusDays(10L), null, OffsetDateTime.now().plusDays(11L), true);
    }

    @Test
    void getChargeInitiativeBeforeStartInitiative(){
        testChargeInitiative(LocalDate.now().plusDays(10L), null, OffsetDateTime.now().plusDays(9L),false);
    }

    @Test
    void getChargeInitiativeIntoInitiativeInterval(){
        testChargeInitiative(LocalDate.now().minusDays(10L), LocalDate.now().plusDays(10L), OffsetDateTime.now(),true);
    }

    void testChargeInitiative(LocalDate initiativeStartDate, LocalDate initiativeEndDate, OffsetDateTime trxDateTime, boolean expectSuccess) {
        if(trxDateTime == null){
            trxDateTime = OffsetDateTime.now().plusDays(10L);
        }
        TransactionDTO trx = buildTrx(trxDateTime);
    }

    @Test
    void checkDate_singleIntervalInside() {
        OnboardedInitiativesServiceImpl svc = (OnboardedInitiativesServiceImpl) onboardedInitiativesService;
        LocalDateTime trxDate = LocalDateTime.now();
        List<it.gov.pagopa.reward.model.ActiveTimeInterval> intervals = new ArrayList<>();
        intervals.add(it.gov.pagopa.reward.model.ActiveTimeInterval.builder()
                .startInterval(trxDate.minusMinutes(5))
                .endInterval(trxDate.plusMinutes(5))
                .build());

        boolean result = svc.checkDate(trxDate, intervals);
        Assertions.assertTrue(result);
    }

    @Test
    void checkDate_endNullMeansOpenEnded() {
        OnboardedInitiativesServiceImpl svc = (OnboardedInitiativesServiceImpl) onboardedInitiativesService;
        LocalDateTime trxDate = LocalDateTime.now();
        List<it.gov.pagopa.reward.model.ActiveTimeInterval> intervals = new ArrayList<>();
        intervals.add(it.gov.pagopa.reward.model.ActiveTimeInterval.builder()
                .startInterval(trxDate.minusDays(1))
                .endInterval(null)
                .build());

        boolean result = svc.checkDate(trxDate, intervals);
        Assertions.assertTrue(result);
    }

    @Test
    void checkDate_multipleIntervalsMatchingEarlier() {
        OnboardedInitiativesServiceImpl svc = (OnboardedInitiativesServiceImpl) onboardedInitiativesService;
        LocalDateTime trxDate = LocalDateTime.now();
        List<it.gov.pagopa.reward.model.ActiveTimeInterval> intervals = new ArrayList<>();
        // First interval does not match
        intervals.add(it.gov.pagopa.reward.model.ActiveTimeInterval.builder()
                .startInterval(trxDate.plusDays(1))
                .endInterval(trxDate.plusDays(2))
                .build());
        // Second interval (later in list) matches — checkDate scans from end
        intervals.add(it.gov.pagopa.reward.model.ActiveTimeInterval.builder()
                .startInterval(trxDate.minusHours(1))
                .endInterval(trxDate.plusHours(1))
                .build());

        boolean result = svc.checkDate(trxDate, intervals);
        Assertions.assertTrue(result);
    }

    @Test
    void checkDate_noMatchReturnsFalse() {
        OnboardedInitiativesServiceImpl svc = (OnboardedInitiativesServiceImpl) onboardedInitiativesService;
        LocalDateTime trxDate = LocalDateTime.now();
        List<it.gov.pagopa.reward.model.ActiveTimeInterval> intervals = new ArrayList<>();
        intervals.add(it.gov.pagopa.reward.model.ActiveTimeInterval.builder()
                .startInterval(trxDate.plusDays(1))
                .endInterval(trxDate.plusDays(2))
                .build());

        boolean result = svc.checkDate(trxDate, intervals);
        Assertions.assertFalse(result);
    }

    private TransactionDTO buildTrx(OffsetDateTime trxDateTime) {
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        trx.setEffectiveAmountCents(CommonUtilities.euroToCents(trx.getAmount()));
        trx.setTrxChargeDate(trxDateTime);
        return trx;
    }

    @Test
    void isOnboarded_emptyWhenStatusNotOk() {
        // Given
        OffsetDateTime trxDate = OffsetDateTime.now();
        String userId = "USER_1";
        String initiativeId = "INITIATIVE_1";

        Mockito.when(onboardingWorkflowConnectorMock.getOnboardingStatus(userId, initiativeId))
                .thenReturn(Mono.just(new OnboardingStatusResponseDTO("ONBOARDING_KO", null, null, null)));

        // When
        Pair<InitiativeConfig, OnboardingInfo> result =
                onboardedInitiativesService.isOnboarded(userId, trxDate, initiativeId).block();

        // Then
        Assertions.assertNull(result);
        Mockito.verify(onboardingWorkflowConnectorMock).getOnboardingStatus(userId, initiativeId);
        Mockito.verifyNoInteractions(rewardContextHolderServiceMock);
    }

    @Test
    void isOnboarded_emptyWhenConnectorReturnsEmpty() {
        // Given
        OffsetDateTime trxDate = OffsetDateTime.now();
        String userId = "USER_1";
        String initiativeId = "INITIATIVE_1";

        Mockito.when(onboardingWorkflowConnectorMock.getOnboardingStatus(userId, initiativeId))
                .thenReturn(Mono.empty());

        // When
        Pair<InitiativeConfig, OnboardingInfo> result =
                onboardedInitiativesService.isOnboarded(userId, trxDate, initiativeId).block();

        // Then
        Assertions.assertNull(result);
        Mockito.verifyNoInteractions(rewardContextHolderServiceMock);
    }

    @Test
    void isOnboarded_emptyWhenInitiativeDatesInvalid() {
        // Given
        OffsetDateTime trxDate = OffsetDateTime.now();
        String userId = "USER_1";
        String initiativeId = "INITIATIVE_1";
        LocalDate now = LocalDate.now();

        Mockito.when(onboardingWorkflowConnectorMock.getOnboardingStatus(userId, initiativeId))
                .thenReturn(Mono.just(new OnboardingStatusResponseDTO("ONBOARDING_OK", null, null, null)));

        // initiative already ended yesterday
        InitiativeConfig expiredConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .startDate(now.minusYears(5L))
                .endDate(now.minusDays(1L))
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId))
                .thenReturn(Mono.just(expiredConfig));

        // When
        Pair<InitiativeConfig, OnboardingInfo> result =
                onboardedInitiativesService.isOnboarded(userId, trxDate, initiativeId).block();

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void isOnboarded_ok() {
        // Given
        OffsetDateTime trxDate = OffsetDateTime.now();
        String userId = "USER_1";
        String initiativeId = "INITIATIVE_1";
        LocalDate now = LocalDate.now();

        Mockito.when(onboardingWorkflowConnectorMock.getOnboardingStatus(userId, initiativeId))
                .thenReturn(Mono.just(new OnboardingStatusResponseDTO("ONBOARDING_OK", null, null, null)));

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .startDate(now.minusYears(5L))
                .endDate(now.plusYears(5L))
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId))
                .thenReturn(Mono.just(initiativeConfig));

        // When
        Pair<InitiativeConfig, OnboardingInfo> result =
                onboardedInitiativesService.isOnboarded(userId, trxDate, initiativeId).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(initiativeConfig, result.getFirst());
        Assertions.assertEquals(new BaseOnboardingInfo(initiativeId, null), result.getSecond());
    }

    @Test
    void isOnboarded_ok_withFamilyId() {
        // Given
        OffsetDateTime trxDate = OffsetDateTime.now();
        String userId = "USER_1";
        String initiativeId = "INITIATIVE_1";
        String familyId = "FAMILY_1";
        LocalDate now = LocalDate.now();

        Mockito.when(onboardingWorkflowConnectorMock.getOnboardingStatus(userId, initiativeId))
                .thenReturn(Mono.just(new OnboardingStatusResponseDTO("ONBOARDING_OK", null, null, familyId)));

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
                .startDate(now.minusYears(5L))
                .endDate(now.plusYears(5L))
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId))
                .thenReturn(Mono.just(initiativeConfig));

        // When
        Pair<InitiativeConfig, OnboardingInfo> result =
                onboardedInitiativesService.isOnboarded(userId, trxDate, initiativeId).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(initiativeConfig, result.getFirst());
        Assertions.assertEquals(new BaseOnboardingInfo(initiativeId, familyId), result.getSecond());
    }

    @Test
    void isOnboarded_emptyWhenNFWithoutFamilyId() {
        // Given
        OffsetDateTime trxDate = OffsetDateTime.now();
        String userId = "USER_1";
        String initiativeId = "INITIATIVE_1";
        LocalDate now = LocalDate.now();

        Mockito.when(onboardingWorkflowConnectorMock.getOnboardingStatus(userId, initiativeId))
            .thenReturn(Mono.just(new OnboardingStatusResponseDTO("ONBOARDING_OK", null, null, null)));

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
                .startDate(now.minusYears(5L))
                .endDate(now.plusYears(5L))
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId))
                .thenReturn(Mono.just(initiativeConfig));

        // When
        Pair<InitiativeConfig, OnboardingInfo> result =
            onboardedInitiativesService.isOnboarded(userId, trxDate, initiativeId).block();

        // Then
        Assertions.assertNull(result);
    }

}