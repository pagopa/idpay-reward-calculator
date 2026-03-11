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
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
        testChargeInitiative(null);
    }

    @Test
    void getChargeInitiativesWhenEndEnd(){
        testChargeInitiative(OffsetDateTime.now().plusDays(10));
    }
    @Test
    void getChargeInitiativesWhenFutureEndEnd(){
        testChargeInitiative(OffsetDateTime.now().plusDays(10));
    }

    @Test
    void getChargeInitiativesWhenPastEndEnd(){
        testChargeInitiative(OffsetDateTime.now().plusDays(10));
    }

    @Test
    void getChargeInitiativesWhenStart(){
        testChargeInitiative(OffsetDateTime.now().plusDays(10L));
    }

    @Test
    void getChargeInitiativeAfterStartInitiative(){
        testChargeInitiative(OffsetDateTime.now().plusDays(11L));
    }

    @Test
    void getChargeInitiativeBeforeStartInitiative(){
        testChargeInitiative(OffsetDateTime.now().plusDays(9L));
    }

    @Test
    void getChargeInitiativeIntoInitiativeInterval(){
        testChargeInitiative(OffsetDateTime.now());
    }

    void testChargeInitiative(OffsetDateTime trxDateTime) {
        if(trxDateTime == null) {
            trxDateTime = OffsetDateTime.now().plusDays(10L);
        }
        TransactionDTO trx = buildTrx(trxDateTime);

        Assertions.assertNotNull(trx);
        Assertions.assertEquals(OperationType.CHARGE, trx.getOperationTypeTranscoded());
        Assertions.assertEquals(CommonUtilities.euroToCents(trx.getAmount()), trx.getEffectiveAmountCents());
        Assertions.assertEquals(trxDateTime, trx.getTrxChargeDate());
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