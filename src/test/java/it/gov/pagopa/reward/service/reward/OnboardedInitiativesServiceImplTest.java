package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
@Slf4j
class OnboardedInitiativesServiceImplTest {

    @Mock private HpanInitiativesRepository hpanInitiativesRepositoryMock;
    @Mock private RewardContextHolderService rewardContextHolderServiceMock;

    private OnboardedInitiativesService onboardedInitiativesService;


    private final String hpan = "5c6bda1b1f5f6238dcba70f9f4b5a77671eb2b1563b0ca6d15d14c649a9b7ce0";
    private final OffsetDateTime trxDate = OffsetDateTime.now().plusDays(6L);

    @BeforeEach
    public void init(){
        onboardedInitiativesService = new OnboardedInitiativesServiceImpl(hpanInitiativesRepositoryMock, rewardContextHolderServiceMock);
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

    void testChargeInitiative(LocalDate initiativeStartDate, LocalDate initiativeEndDate, OffsetDateTime trxDateTime, boolean expectSuccess) {
        if(trxDateTime == null){
            trxDateTime = OffsetDateTime.now().plusDays(10L);
        }
        TransactionDTO trx = buildTrx(trxDateTime, hpan);

        testGetInitiativesPrivateMethod(trx,initiativeStartDate, initiativeEndDate, expectSuccess);
    }
    void testGetInitiativesPrivateMethod(TransactionDTO trx, LocalDate initiativeStartDate, LocalDate initiativeEndDate, boolean expectSuccess) {
        // Given
        Integer bias = 1;
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(bias);

        Mockito.when(hpanInitiativesRepositoryMock.findById(Mockito.same(hpan))).thenReturn(Mono.just(hpanInitiatives));

        final String mockedInitiativeId = hpanInitiatives.getOnboardedInitiatives().get(0).getInitiativeId();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(mockedInitiativeId)).thenReturn(
                InitiativeConfig.builder()
                        .initiativeId(mockedInitiativeId)
                        .startDate(initiativeStartDate)
                        .endDate(initiativeEndDate)
                        .build());


        // When
        List<String> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        if(expectSuccess) {
            Mockito.verify(hpanInitiativesRepositoryMock).findById(Mockito.same(hpan));
            Assertions.assertEquals(1, result.size());
            Assertions.assertTrue(result.contains(String.format("INITIATIVE_%d", bias)));
        } else {
            checkInitiativeNotFound(hpanInitiativesRepositoryMock, hpan, result);
        }
    }

    private TransactionDTO buildTrx(OffsetDateTime trxDateTime, String hpan) {
        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        trx.setEffectiveAmount(trx.getAmount());
        trx.setTrxChargeDate(trxDateTime);
        trx.setHpan(hpan);
        return trx;
    }

    @Test
    void getInitiativesHpanNotFound() {
        // Given
        TransactionDTO trx = buildTrx(trxDate, hpan);

        Mockito.when(hpanInitiativesRepositoryMock.findById(Mockito.same(hpan))).thenReturn(Mono.empty());

        // When
        List<String> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        checkInitiativeNotFound(hpanInitiativesRepositoryMock, hpan, result);
    }

    private void checkInitiativeNotFound(HpanInitiativesRepository hpanInitiativesRepository, String hpanMock, List<String> result) {
        Mockito.verify(hpanInitiativesRepository).findById(Mockito.same(hpanMock));
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getNotOnboardedInitiatives() {
        // Given
        TransactionDTO trx = buildTrx(trxDate, hpan);

        Integer bias = 1;
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceWithoutInitiative(bias);

        Mockito.when(hpanInitiativesRepositoryMock.findById(Mockito.same(hpan))).thenReturn(Mono.just(hpanInitiatives));

        // When
        List<String> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        checkInitiativeNotFound(hpanInitiativesRepositoryMock, hpan, result);
    }

    @Test
    void getNotIntitiaveTrxNotInActiveInterval() {
        // Given
        TransactionDTO trx = buildTrx(trxDate, hpan);

        Integer bias = 1;
        HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstanceNotInActiveInterval(bias);

        Mockito.when(hpanInitiativesRepositoryMock.findById(Mockito.same(hpan))).thenReturn(Mono.just(hpanInitiatives));

        final String mockedInitiativeId = hpanInitiatives.getOnboardedInitiatives().get(0).getInitiativeId();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(mockedInitiativeId)).thenReturn(InitiativeConfig.builder().initiativeId(mockedInitiativeId).build());

        // When
        List<String> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        checkInitiativeNotFound(hpanInitiativesRepositoryMock, hpan, result);
    }

    @Test
    void getCompleteReverseNoChargeRewarded() {
        // Given
        TransactionDTO trx = buildTrx(trxDate, hpan);
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        trx.setEffectiveAmount(BigDecimal.ZERO);

        // When
        List<String> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getCompleteReverse() {
        // Given
        TransactionDTO trx = buildTrx(trxDate, hpan);
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        trx.setEffectiveAmount(BigDecimal.ZERO);
        trx.setRefundInfo(new RefundInfo());
        trx.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE2REVERSE", new RefundInfo.PreviousReward("INITIATIVE2REVERSE", "ORGANIZATION", BigDecimal.ONE)));

        // When
        List<String> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        Assertions.assertEquals(List.of("INITIATIVE2REVERSE"), result);
    }

    @Test
    void getCompleteReverseCapped() {
        // Given
        TransactionDTO trx = buildTrx(trxDate, hpan);
        trx.setOperationTypeTranscoded(OperationType.REFUND);
        trx.setEffectiveAmount(BigDecimal.ZERO);
        trx.setRefundInfo(new RefundInfo());
        trx.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE2REVERSE", new RefundInfo.PreviousReward("INITIATIVE2REVERSE", "ORGANIZATION", BigDecimal.ZERO)));

        // When
        List<String> result = onboardedInitiativesService.getInitiatives(trx).collectList().block();
        Assertions.assertNotNull(result);

        // Then
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void getPartialReverseCapped() {
        // Given
        TransactionDTO trx = buildTrx(trxDate, hpan);
        trx.setOperationTypeTranscoded(OperationType.REFUND);

        testGetInitiativesPrivateMethod(trx, null, null, true);
    }
}