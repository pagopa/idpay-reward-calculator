package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedService;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedServiceImpl;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class InitiativeEvaluatorFacadeServiceTest {

    @BeforeAll
    public static void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(RewardConstants.ZONEID));
    }

    @Mock
    private TransactionProcessedService transactionProcessedService = Mockito.mock(TransactionProcessedServiceImpl.class);
    @Mock
    private UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock
    private UserInitiativeCountersUpdateService userInitiativeCountersUpdateServiceMock;
    @Mock
    private InitiativesEvaluatorService initiativesEvaluatorServiceMock;
    @Mock
    private ErrorNotifierService errorNotifierServiceMock;

    private InitiativesEvaluatorFacadeServiceImpl initiativesEvaluatorFacadeService;

    private final Transaction2RewardTransactionMapper rewardTransactionMapper = new Transaction2RewardTransactionMapper();
    private final Transaction2TransactionProcessedMapper reward2ProcessedMapper = new Transaction2TransactionProcessedMapper();

    @BeforeEach
    public void initMocks() {
        initiativesEvaluatorFacadeService = new InitiativesEvaluatorFacadeServiceImpl(
                userInitiativeCountersRepositoryMock,
                initiativesEvaluatorServiceMock,
                userInitiativeCountersUpdateServiceMock,
                transactionProcessedService,
                rewardTransactionMapper);
    }

    @Test
    void test() {
        // Given
        TransactionDTO trx = buildTrx(0);
        TransactionDTO invalidTrx = buildTrx(1);
        TransactionDTO trxPartialRefund = buildTrx(3);
        TransactionDTO trxTotalRefund = buildTrx(4);
        TransactionDTO trxTotalRefundNoCharge = buildTrx(5);
        List<TransactionDTO> trxs = List.of(trx, invalidTrx, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);

        invalidTrx.setEffectiveAmount(invalidTrx.getAmount().negate());

        List<String> initiatives = List.of("INITIATIVE");

        mockUseCases(trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);

        // When
        @SuppressWarnings("ConstantConditions") List<RewardTransactionDTO> result = trxs.stream().map(t -> initiativesEvaluatorFacadeService.evaluate(t, initiatives).block()).toList();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(trxs.size(), result.size());

        assertRejectionReasons(result, trx, null);
        assertRejectionReasons(result, invalidTrx, "INVALID_AMOUNT");
        assertRejectionReasons(result, trxPartialRefund, null);
        assertRejectionReasons(result, trxTotalRefund, null);
        assertRejectionReasons(result, trxTotalRefundNoCharge, "NO_ACTIVE_INITIATIVES");

        verifyUserInitiativeCounterFindByIdCalls(trx, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);
        verifyInitiativeEvaluatorCalls(initiatives, trx, trxPartialRefund);
        verifyTransactionProcessedSaveCalls(trx);
        verifyUserInitiativeCounterSaveCalls(trx, trxPartialRefund, trxTotalRefund);

        Mockito.verifyNoMoreInteractions(transactionProcessedService, userInitiativeCountersRepositoryMock, initiativesEvaluatorServiceMock);

        Mockito.verifyNoInteractions(errorNotifierServiceMock);

        checkPartialRefundResult(result.get(2));
        checkTotalRefundResult(result.get(3));
    }

    private void assertRejectionReasons(List<RewardTransactionDTO> result, TransactionDTO trx, String expectedRejectionReason) {
        final RewardTransactionDTO rewardedTrx = result.stream()
                .filter(r->r.getIdTrxAcquirer().equals(trx.getIdTrxAcquirer()))
                .findFirst().orElseThrow();
        Assertions.assertEquals(expectedRejectionReason != null ? List.of(expectedRejectionReason) : Collections.emptyList(), rewardedTrx.getRejectionReasons());
        Assertions.assertEquals(expectedRejectionReason != null ? "REJECTED" : "REWARDED", rewardedTrx.getStatus());
    }

    private TransactionDTO buildTrx(int i) {
        final TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
        trx.setAmount(BigDecimal.valueOf(i + 1));
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        trx.setTrxChargeDate(trx.getTrxDate());
        trx.setEffectiveAmount(trx.getAmount());
        return trx;
    }

    private void mockUseCases(TransactionDTO trxPartialReverse, TransactionDTO trxTotalRefund, TransactionDTO trxTotalRefundNoCharge) {
        Mockito.when(transactionProcessedService.save(Mockito.any())).thenAnswer(i -> Mono.just(reward2ProcessedMapper.apply(i.getArgument(0))));

        Mockito.when(userInitiativeCountersRepositoryMock.findById(Mockito.<String>any())).thenReturn(Mono.empty());
        Mockito.when(userInitiativeCountersRepositoryMock.save(Mockito.any())).thenReturn(Mono.empty());

        Mockito.when(initiativesEvaluatorServiceMock.evaluateInitiativesBudgetAndRules(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(i -> rewardTransactionMapper.apply(i.getArgument(0)));


        Mockito.when(initiativesEvaluatorServiceMock.evaluateInitiativesBudgetAndRules(Mockito.eq(trxPartialReverse), Mockito.any(), Mockito.any()))
                .thenAnswer(i -> {
                    RewardTransactionDTO reward = rewardTransactionMapper.apply(i.getArgument(0));
                    reward.setRewards(new HashMap<>(Map.of("INITIATIVE2PARTIALREVERSE", new Reward(BigDecimal.valueOf(9)))));
                    reward.setInitiativeRejectionReasons(new HashMap<>(Map.of("INITIATIVE2REVERSE", List.of("NOT_MORE_REWARDED_FOR_SOME_REASON"))));
                    return reward;
                });

        trxPartialReverse.setOperationTypeTranscoded(OperationType.REFUND);
        trxPartialReverse.setEffectiveAmount(trxPartialReverse.getAmount());
        trxPartialReverse.setRefundInfo(new RefundInfo());
        trxPartialReverse.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE2PARTIALREVERSE", BigDecimal.TEN, "INITIATIVE2REVERSE", BigDecimal.ONE));

        trxTotalRefundNoCharge.setOperationTypeTranscoded(OperationType.REFUND);
        trxTotalRefundNoCharge.setEffectiveAmount(BigDecimal.ZERO);

        trxTotalRefund.setOperationTypeTranscoded(OperationType.REFUND);
        trxTotalRefund.setEffectiveAmount(BigDecimal.ZERO);
        trxTotalRefund.setRefundInfo(new RefundInfo());
        trxTotalRefund.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE2REVERSE", BigDecimal.ONE));
    }

    private void verifyUserInitiativeCounterFindByIdCalls(TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(userInitiativeCountersRepositoryMock).findById(t.getUserId());
        }
    }

    private void verifyInitiativeEvaluatorCalls(List<String> expectedInitiatives, TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(initiativesEvaluatorServiceMock).evaluateInitiativesBudgetAndRules(Mockito.eq(t), Mockito.eq(expectedInitiatives), Mockito.any());
        }
    }

    private void verifyTransactionProcessedSaveCalls(TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(transactionProcessedService).save(rewardTransactionMapper.apply(t));
        }
    }

    private void verifyUserInitiativeCounterSaveCalls(TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(userInitiativeCountersRepositoryMock).save(Mockito.argThat(i -> i.getUserId().equals(t.getUserId())));
        }
    }

    private void checkPartialRefundResult(RewardTransactionDTO rewardTransactionDTO) {
        Assertions.assertEquals(new Reward(BigDecimal.valueOf(9), BigDecimal.valueOf(-1), false), rewardTransactionDTO.getRewards().get("INITIATIVE2PARTIALREVERSE"));
        Assertions.assertEquals(new Reward(BigDecimal.valueOf(-1)), rewardTransactionDTO.getRewards().get("INITIATIVE2REVERSE"));
        Assertions.assertEquals(Map.of("INITIATIVE2REVERSE", List.of("NOT_MORE_REWARDED_FOR_SOME_REASON")), rewardTransactionDTO.getInitiativeRejectionReasons());
    }

    private void checkTotalRefundResult(RewardTransactionDTO rewardTransactionDTO) {
        Assertions.assertEquals(new Reward(BigDecimal.valueOf(-1)), rewardTransactionDTO.getRewards().get("INITIATIVE2REVERSE"));
    }
}