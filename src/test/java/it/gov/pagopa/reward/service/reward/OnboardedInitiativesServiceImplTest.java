package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.repository.secondary.UserInitiativesRepository;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OnboardingStatus;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.OnboardingInfo;
import it.gov.pagopa.reward.model.UserInitiatives;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.UserInitiativesFaker;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@Slf4j
class OnboardedInitiativesServiceImplTest {

    @Mock private UserInitiativesRepository userInitiativesRepositoryMock;
    @Mock private RewardContextHolderService rewardContextHolderServiceMock;

    private OnboardedInitiativesService onboardedInitiativesService;

    private final OffsetDateTime trxDate = OffsetDateTime.now().plusDays(6L);

    @BeforeEach
    public void init(){
        onboardedInitiativesService = new OnboardedInitiativesServiceImpl(userInitiativesRepositoryMock, rewardContextHolderServiceMock);
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
        testGetInitiativesPrivateMethod(trx, initiativeStartDate, initiativeEndDate, expectSuccess);
    }

    void testGetInitiativesPrivateMethod(TransactionDTO trx, LocalDate initiativeStartDate, LocalDate initiativeEndDate, boolean expectSuccess) {
        // Given
        Integer bias = 1;
        UserInitiatives userInitiatives = UserInitiativesFaker.mockInstance(bias);

        Mockito.when(userInitiativesRepositoryMock.findById(Mockito.same(trx.getUserId()))).thenReturn(Mono.just(userInitiatives));

        final String mockedInitiativeId = userInitiatives.getOnboardedInitiatives().get(0).getInitiativeId();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(mockedInitiativeId)).thenReturn(
                Mono.just(InitiativeConfig.builder()
                        .initiativeId(mockedInitiativeId)
                        .startDate(initiativeStartDate)
                        .endDate(initiativeEndDate)
                        .build()));

        // When
        List<InitiativeConfig> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);
        List<String> resultIds = result.stream().map(InitiativeConfig::getInitiativeId).toList();

        // Then
        if(expectSuccess) {
            Mockito.verify(userInitiativesRepositoryMock).findById(Mockito.same(trx.getUserId()));
            Assertions.assertEquals(1, resultIds.size());
            Assertions.assertTrue(resultIds.contains(String.format("INITIATIVE_%d", bias)));
        } else {
            checkInitiativeNotFound(userInitiativesRepositoryMock, trx, result);
        }
    }

    private TransactionDTO buildTrx(OffsetDateTime trxDateTime) {
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        trx.setEffectiveAmountCents(CommonUtilities.euroToCents(trx.getAmount()));
        trx.setTrxChargeDate(trxDateTime);
        return trx;
    }

    @Test
    void getInitiativesHpanNotFound() {
        // Given
        TransactionDTO trx = buildTrx(trxDate);

        Mockito.when(userInitiativesRepositoryMock.findById(Mockito.same(trx.getUserId()))).thenReturn(Mono.empty());

        // When
        List<InitiativeConfig> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        checkInitiativeNotFound(userInitiativesRepositoryMock, trx, result);
    }

    private void checkInitiativeNotFound(UserInitiativesRepository userInitiativesRepository, TransactionDTO trx, List<InitiativeConfig> result) {
        Mockito.verify(userInitiativesRepository).findById(Mockito.same(trx.getUserId()));
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getNotOnboardedInitiatives() {
        // Given
        TransactionDTO trx = buildTrx(trxDate);

        Integer bias = 1;
        UserInitiatives userInitiatives = UserInitiativesFaker.mockInstanceWithoutInitiative(bias);

        Mockito.when(userInitiativesRepositoryMock.findById(Mockito.same(trx.getUserId()))).thenReturn(Mono.just(userInitiatives));

        // When
        List<InitiativeConfig> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        checkInitiativeNotFound(userInitiativesRepositoryMock, trx, result);
    }

    @Test
    void getNotIntitiaveTrxNotInActiveInterval() {
        // Given
        TransactionDTO trx = buildTrx(trxDate);

        Integer bias = 1;
        UserInitiatives userInitiatives = UserInitiativesFaker.mockInstanceNotInActiveInterval(bias);

        Mockito.when(userInitiativesRepositoryMock.findById(Mockito.same(trx.getUserId()))).thenReturn(Mono.just(userInitiatives));

        final String mockedInitiativeId = userInitiatives.getOnboardedInitiatives().get(0).getInitiativeId();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(mockedInitiativeId)).thenReturn(Mono.just(InitiativeConfig.builder().initiativeId(mockedInitiativeId).build()));

        // When
        List<InitiativeConfig> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        checkInitiativeNotFound(userInitiativesRepositoryMock, trx, result);
    }

    @Test
    void getCompleteReverseNoChargeRewarded() {
        // Given
        TransactionDTO trx = buildTrx(trxDate);
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        trx.setEffectiveAmountCents(0L);

        // When
        List<InitiativeConfig> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getCompleteReverse() {
        // Given
        TransactionDTO trx = buildTrx(trxDate);
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        trx.setEffectiveAmountCents(0L);
        trx.setRefundInfo(new RefundInfo());
        trx.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE2REVERSE", new RefundInfo.PreviousReward("INITIATIVE2REVERSE", "ORGANIZATION", 1_00L)));

        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig("INITIATIVE2REVERSE")).thenReturn(Mono.just(InitiativeConfig.builder().initiativeId("INITIATIVE2REVERSE").build()));

        // When
        List<InitiativeConfig> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        Assertions.assertEquals(List.of("INITIATIVE2REVERSE"), result.stream().map(InitiativeConfig::getInitiativeId).toList());
    }

    @Test
    void getPartialReverseCapped() {
        // Given
        TransactionDTO trx = buildTrx(trxDate);
        trx.setOperationTypeTranscoded(OperationType.REFUND);

        testGetInitiativesPrivateMethod(trx, null, null, true);
    }

    @Test
    void isOnboardedEmpty() {
        // Given
        OffsetDateTime trxDate = OffsetDateTime.now();
        String initiativeId = "INITIATIVEID";
        LocalDate now = LocalDate.now();
        UserInitiatives userInitiatives = UserInitiativesFaker.mockInstance(1);
        Mockito.when(userInitiativesRepositoryMock.findById(userInitiatives.getUserId())).thenReturn(Mono.just(userInitiatives));

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVE_1")
                .startDate(now.minusYears(5L))
                .startDate(now.plusYears(5L))
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig("INITIATIVE_1")).thenReturn(Mono.just(initiativeConfig));
        // When
        Pair<InitiativeConfig, OnboardingInfo> result = onboardedInitiativesService.isOnboarded(userInitiatives.getUserId(), trxDate, initiativeId).block();
        // Then
        Assertions.assertNull(result);
    }

    @Test
    void isOnboardedOK_withFamilyId() {
        // Given
        OffsetDateTime trxDate = OffsetDateTime.now();
        LocalDate now = LocalDate.now();
        UserInitiatives userInitiatives = UserInitiativesFaker.mockInstance(1);
        userInitiatives.getOnboardedInitiatives().get(0).setFamilyId("FAMILY_ID");
        String initiativeId = userInitiatives.getOnboardedInitiatives().get(0).getInitiativeId();
        Mockito.when(userInitiativesRepositoryMock.findById(userInitiatives.getUserId())).thenReturn(Mono.just(userInitiatives));

        InitiativeConfig initiativeConfig = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF)
                .startDate(now.minusYears(5L))
                .endDate(now.plusYears(5L))
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(initiativeId)).thenReturn(Mono.just(initiativeConfig));
        // When
        Pair<InitiativeConfig, OnboardingInfo> result = onboardedInitiativesService.isOnboarded(userInitiatives.getUserId(), trxDate, initiativeId).block();
        // Then
        Pair<InitiativeConfig, OnboardingInfo> expectedPair = Pair.of(initiativeConfig, userInitiatives.getOnboardedInitiatives().get(0));
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedPair, result);
    }

    @Test
    void onboardedInitiativeInactive(){
        // Given
        TransactionDTO trx = buildTrx(trxDate);

        Integer bias = 1;
        UserInitiatives userInitiatives = UserInitiativesFaker.mockInstanceWithoutInitiative(bias);
        OnboardedInitiative onboardedInitiative = OnboardedInitiative.builder()
                .initiativeId(String.format("INITIATIVE_%d",bias))
                .status(OnboardingStatus.INACTIVE)
                .activeTimeIntervals(new ArrayList<>()).build();
        userInitiatives.setOnboardedInitiatives(List.of(onboardedInitiative));

        Mockito.when(userInitiativesRepositoryMock.findById(Mockito.same(trx.getUserId()))).thenReturn(Mono.just(userInitiatives));

        // When
        List<InitiativeConfig> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        checkInitiativeNotFound(userInitiativesRepositoryMock, trx, result);
    }
}