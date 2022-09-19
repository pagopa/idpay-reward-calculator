package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedService;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedServiceImpl;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
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
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@ExtendWith(MockitoExtension.class)
class InitiativeEvaluatorFacadeServiceTest {

    @BeforeAll
    public static void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
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
        TransactionDTO trxPartialRefund = buildTrx(3);
        TransactionDTO trxTotalRefund = buildTrx(4);
        TransactionDTO trxTotalRefundNoCharge = buildTrx(5);
        List<TransactionDTO> trxs = List.of(trx, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);

        List<String> initiatives = List.of("INITIATIVE");

        mockUseCases(trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);

        // When
        List<RewardTransactionDTO> result = trxs.stream().map(t -> initiativesEvaluatorFacadeService.evaluate(t, initiatives).block()).toList();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.size());

        assertRejectionReasons(result, 0, null);
        assertRejectionReasons(result, 1, null);
        assertRejectionReasons(result, 2, null);
        assertRejectionReasons(result, 3, "NO_ACTIVE_INITIATIVES");

        verifyUserInitiativeCounterFindByIdCalls(trx, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);
        verifyInitiativeEvaluatorCalls(initiatives, trx, trxPartialRefund);
        verifyTransactionProcessedSaveCalls(trx);
        verifyUserInitiativeCounterSaveCalls(trx, trxPartialRefund, trxTotalRefund);

        Mockito.verifyNoMoreInteractions(transactionProcessedService, userInitiativeCountersRepositoryMock, initiativesEvaluatorServiceMock);

        Mockito.verifyNoInteractions(errorNotifierServiceMock);

    }

    private void assertRejectionReasons(List<RewardTransactionDTO> result, int index, String expectedRejectionReason) {
        Assertions.assertEquals(expectedRejectionReason != null ? List.of(expectedRejectionReason) : Collections.emptyList(), result.get(index).getRejectionReasons());
        Assertions.assertEquals(expectedRejectionReason != null ? "REJECTED" : "REWARDED", result.get(index).getStatus());
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
        Mockito.when(transactionProcessedService.save(Mockito.any())).thenAnswer(i -> Mono.just(i.getArgument(0)));

        Mockito.when(userInitiativeCountersRepositoryMock.findById(Mockito.<String>any())).thenReturn(Mono.empty());
        Mockito.when(userInitiativeCountersRepositoryMock.save(Mockito.any())).thenReturn(Mono.empty());

        Mockito.when(initiativesEvaluatorServiceMock.evaluateInitiativesBudgetAndRules(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(i -> rewardTransactionMapper.apply(i.getArgument(0)));

        trxPartialReverse.setOperationTypeTranscoded(OperationType.REFUND);
        trxPartialReverse.setEffectiveAmount(trxPartialReverse.getAmount());
        trxPartialReverse.setRefundInfo(new RefundInfo());
        trxPartialReverse.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE2REVERSE", BigDecimal.ONE));

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

}