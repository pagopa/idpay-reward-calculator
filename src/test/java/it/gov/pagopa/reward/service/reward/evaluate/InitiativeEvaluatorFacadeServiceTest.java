package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.LockService;
import it.gov.pagopa.reward.service.reward.TransactionProcessedService;
import it.gov.pagopa.reward.service.reward.TransactionProcessedServiceImpl;
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
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class InitiativeEvaluatorFacadeServiceTest {

    public static final int LOCK_SERVICE_BUKET_SIZE = 1000;

    @BeforeAll
    public static void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
    }

    @Mock
    private LockService lockServiceMock = Mockito.mock(LockService.class);
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
                lockServiceMock,
                userInitiativeCountersRepositoryMock,
                initiativesEvaluatorServiceMock,
                userInitiativeCountersUpdateServiceMock,
                transactionProcessedService,
                rewardTransactionMapper);

        Mockito.when(lockServiceMock.getBuketSize()).thenReturn(LOCK_SERVICE_BUKET_SIZE);
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

        verifyLockAcquireReleaseCalls(trx, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);
        verifyUserInitiativeCounterFindByIdCalls(trx, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);
        verifyInitiativeEvaluatorCalls(initiatives, trx, trxPartialRefund);
        verifyTransactionProcessedSaveCalls(trx);
        verifyUserInitiativeCounterSaveCalls(trx, trxPartialRefund, trxTotalRefund);

        Mockito.verifyNoMoreInteractions(transactionProcessedService, userInitiativeCountersRepositoryMock, initiativesEvaluatorServiceMock, lockServiceMock);

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

    private void verifyLockAcquireReleaseCalls(TransactionDTO... expectedTrxs) {
        Mockito.verify(lockServiceMock, Mockito.times(expectedTrxs.length)).getBuketSize();

        final List<Integer> expectedLockIds = Arrays.stream(expectedTrxs).map(t -> initiativesEvaluatorFacadeService.calculateLockId(t.getUserId())).toList();

        Mockito.verify(lockServiceMock, Mockito.times(expectedTrxs.length*2)).getBuketSize();

        Mockito.verify(lockServiceMock, Mockito.times(expectedTrxs.length)).acquireLock(Mockito.anyInt());
        final List<Integer> acquiredLockIds = Mockito.mockingDetails(lockServiceMock).getInvocations().stream()
                .filter(i -> "acquireLock".equals(i.getMethod().getName()))
                .map(i -> (Integer) i.getArgument(0))
                .toList();

        Assertions.assertEquals(
                expectedLockIds,
                acquiredLockIds
        );

        Mockito.verify(lockServiceMock, Mockito.times(expectedTrxs.length)).releaseLock(Mockito.anyInt());
        final List<Integer> releasedLocks = Mockito.mockingDetails(lockServiceMock).getInvocations().stream()
                .filter(i -> "releaseLock".equals(i.getMethod().getName()))
                .map(i -> (Integer) i.getArgument(0))
                .toList();

        Assertions.assertEquals(
                expectedLockIds,
                releasedLocks
        );
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

    @Test
    void testTrxLockIdCalculation() {
        final Map<Integer, Long> lockId2Count = IntStream.range(0, LOCK_SERVICE_BUKET_SIZE)
                .mapToObj(i -> initiativesEvaluatorFacadeService.calculateLockId(UUID.nameUUIDFromBytes((i + "").getBytes(StandardCharsets.UTF_8)).toString()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        lockId2Count.forEach((lockId, count) -> {
            Assertions.assertTrue(lockId < LOCK_SERVICE_BUKET_SIZE && lockId >= 0);
            Assertions.assertTrue(count < 10, "LockId %d hit too times: %d".formatted(lockId, count));
        });
    }
}