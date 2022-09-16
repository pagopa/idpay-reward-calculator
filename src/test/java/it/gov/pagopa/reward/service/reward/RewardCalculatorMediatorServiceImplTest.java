package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import it.gov.pagopa.reward.service.LockService;
import it.gov.pagopa.reward.service.reward.evaluate.InitiativesEvaluatorFacadeService;
import it.gov.pagopa.reward.service.reward.ops.OperationTypeHandlerService;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class RewardCalculatorMediatorServiceImplTest {

    public static final int LOCK_SERVICE_BUKET_SIZE = 1000;

    @BeforeAll
    public static void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
    }

    @Mock private LockService lockServiceMock;
    @Mock private TransactionProcessedService transactionProcessedServiceMock;
    @Mock private OperationTypeHandlerService operationTypeHandlerServiceMock;
    @Mock private TransactionValidatorService transactionValidatorServiceMock;
    @Mock private OnboardedInitiativesService onboardedInitiativesServiceMock;
    @Mock private InitiativesEvaluatorFacadeService initiativesEvaluatorFacadeServiceMock;
    @Mock private ErrorNotifierService errorNotifierServiceMock;

    private RewardCalculatorMediatorServiceImpl rewardCalculatorMediatorService;

    private final Transaction2RewardTransactionMapper rewardTransactionMapper = new Transaction2RewardTransactionMapper();

    @BeforeEach
    public void initMocks(){
        rewardCalculatorMediatorService = new RewardCalculatorMediatorServiceImpl(
                lockServiceMock,
                transactionProcessedServiceMock,
                operationTypeHandlerServiceMock,
                transactionValidatorServiceMock,
                onboardedInitiativesServiceMock,
                initiativesEvaluatorFacadeServiceMock,
                rewardTransactionMapper,
                errorNotifierServiceMock,
                TestUtils.objectMapper);

        Mockito.when(lockServiceMock.getBuketSize()).thenReturn(LOCK_SERVICE_BUKET_SIZE);
    }

    private Flux<Message<String>> buildTrxFlux(TransactionDTO... trxs) {
        return Flux.just(trxs)
                .map(TestUtils::jsonSerializer)
                .map(MessageBuilder::withPayload).map(MessageBuilder::build);
    }

    @Test
    void test() {
        // Given
        TransactionDTO trx = buildTrx(0);
        TransactionDTO trxInvalidOpType = buildTrx(1);
        TransactionDTO trxInvalidAmount = buildTrx(2);
        TransactionDTO trxPartialRefund = buildTrx(3);
        TransactionDTO trxTotalRefund = buildTrx(4);
        Flux<Message<String>> trxFlux = buildTrxFlux(trx, trxInvalidOpType, trxInvalidAmount, trxPartialRefund, trxTotalRefund);

        List<String> expectedInitiatives = List.of("INITIATIVE");

        mockUseCases(expectedInitiatives, trxInvalidOpType, trxInvalidAmount, trxPartialRefund, trxTotalRefund);

        // When
        List<RewardTransactionDTO> result = rewardCalculatorMediatorService.execute(trxFlux).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.size());

        assertRejectionReasons(result, trx,null);
        assertRejectionReasons(result, trxInvalidOpType, "REJECTED");
        assertRejectionReasons(result, trxInvalidAmount, "INVALID_AMOUNT");
        assertRejectionReasons(result, trxPartialRefund,null);
        assertRejectionReasons(result, trxTotalRefund,null);

        verifyLockAcquireReleaseCalls(trx, trxInvalidOpType, trxInvalidAmount, trxPartialRefund, trxTotalRefund);
        verifyDuplicateCheckCalls(trx);
        verifyOperationTypeCalls(trx, trxInvalidOpType, trxInvalidAmount, trxPartialRefund, trxTotalRefund);
        verifyOnboardedInitiativeCalls(trx, trxPartialRefund);
        verifyInitiativeEvaluatorCalls(expectedInitiatives,trx, trxPartialRefund);

        Mockito.verifyNoMoreInteractions(transactionProcessedServiceMock, operationTypeHandlerServiceMock, onboardedInitiativesServiceMock, initiativesEvaluatorFacadeServiceMock, lockServiceMock);

        Mockito.verifyNoInteractions(errorNotifierServiceMock);

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
        trx.setAmount(BigDecimal.valueOf(i+1));
        return trx;
    }

    private void mockUseCases(List<String> initiatives, TransactionDTO trxInvalidOpType, TransactionDTO trxInvalidAmount, TransactionDTO trxPartialReverse, TransactionDTO trxTotalRefund) {
        Mockito.when(transactionProcessedServiceMock.checkDuplicateTransactions(Mockito.any())).thenAnswer(i->Mono.just(i.getArgument(0)));

        Mockito.when(operationTypeHandlerServiceMock.handleOperationType(Mockito.any())).thenAnswer(i-> {
            final TransactionDTO trx = i.getArgument(0);
            trx.setOperationTypeTranscoded(OperationType.CHARGE);
            trx.setTrxChargeDate(trx.getTrxDate());
            trx.setEffectiveAmount(trx.getAmount());
            return Mono.just(trx);
        });

        Mockito.when(transactionValidatorServiceMock.validate(Mockito.any())).thenAnswer(i->i.getArgument(0));

        Mockito.when(initiativesEvaluatorFacadeServiceMock.evaluate(Mockito.any(), Mockito.any())).thenAnswer(i->Mono.just(rewardTransactionMapper.apply(i.getArgument(0))));

        Mockito.when(onboardedInitiativesServiceMock.getInitiatives(Mockito.any()))
                .thenReturn(Flux.fromIterable(initiatives));

        Mockito.when(operationTypeHandlerServiceMock.handleOperationType(trxInvalidOpType)).thenAnswer(i-> {
            final TransactionDTO t = i.getArgument(0);
            t.setRejectionReasons(List.of("REJECTED"));
            return Mono.just(t);
        });

        Mockito.when(transactionValidatorServiceMock.validate(trxInvalidAmount)).thenAnswer(i-> {
            final TransactionDTO t = i.getArgument(0);
            t.setRejectionReasons(List.of("INVALID_AMOUNT"));
            return t;
        });

        Mockito.when(operationTypeHandlerServiceMock.handleOperationType(trxPartialReverse)).thenAnswer(i-> {
            final TransactionDTO t = i.getArgument(0);
            t.setOperationTypeTranscoded(OperationType.REFUND);
            t.setEffectiveAmount(t.getAmount());
            t.setRefundInfo(new RefundInfo());
            t.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE2REVERSE", BigDecimal.ONE));
            return Mono.just(t);
        });

        Mockito.when(operationTypeHandlerServiceMock.handleOperationType(trxTotalRefund)).thenAnswer(i-> {
            final TransactionDTO t = i.getArgument(0);
            t.setOperationTypeTranscoded(OperationType.REFUND);
            t.setEffectiveAmount(BigDecimal.ZERO);
            t.setRefundInfo(new RefundInfo());
            t.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE2REVERSE", BigDecimal.ONE));
            return Mono.just(t);
        });
    }

    private void verifyLockAcquireReleaseCalls(TransactionDTO... expectedTrxs) {
        Mockito.verify(lockServiceMock, Mockito.times(expectedTrxs.length)).getBuketSize();

        final List<Integer> expectedLockIds = Arrays.stream(expectedTrxs).map(t -> rewardCalculatorMediatorService.calculateLockId(t.getUserId())).sorted().toList();

        Mockito.verify(lockServiceMock, Mockito.times(expectedTrxs.length*2)).getBuketSize();

        Mockito.verify(lockServiceMock, Mockito.times(expectedTrxs.length)).acquireLock(Mockito.anyInt());
        final List<Integer> acquiredLockIds = Mockito.mockingDetails(lockServiceMock).getInvocations().stream()
                .filter(i -> "acquireLock".equals(i.getMethod().getName()))
                .map(i -> (Integer) i.getArgument(0))
                .sorted()
                .toList();

        Assertions.assertEquals(
                expectedLockIds,
                acquiredLockIds
        );

        Mockito.verify(lockServiceMock, Mockito.times(expectedTrxs.length)).releaseLock(Mockito.anyInt());
        final List<Integer> releasedLocks = Mockito.mockingDetails(lockServiceMock).getInvocations().stream()
                .filter(i -> "releaseLock".equals(i.getMethod().getName()))
                .map(i -> (Integer) i.getArgument(0))
                .sorted()
                .toList();

        Assertions.assertEquals(
                expectedLockIds,
                releasedLocks
        );
    }

    private void verifyDuplicateCheckCalls(TransactionDTO... expectedTrxs){
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(transactionProcessedServiceMock).checkDuplicateTransactions(t);
        }
    }

    private void verifyOperationTypeCalls(TransactionDTO... expectedTrxs){
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(operationTypeHandlerServiceMock).handleOperationType(t);
        }
    }

    private void verifyOnboardedInitiativeCalls(TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(onboardedInitiativesServiceMock).getInitiatives(t);
        }
    }

    private void verifyInitiativeEvaluatorCalls(List<String> expectedInitiatives, TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(initiativesEvaluatorFacadeServiceMock).evaluate(t, expectedInitiatives);
        }
    }

    @Test
    void testTrxLockIdCalculation() {
        final Map<Integer, Long> lockId2Count = IntStream.range(0, LOCK_SERVICE_BUKET_SIZE)
                .mapToObj(i -> rewardCalculatorMediatorService.calculateLockId(UUID.nameUUIDFromBytes((i + "").getBytes(StandardCharsets.UTF_8)).toString()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        lockId2Count.forEach((lockId, count) -> {
            Assertions.assertTrue(lockId < LOCK_SERVICE_BUKET_SIZE && lockId >= 0);
            Assertions.assertTrue(count < 10, "LockId %d hit too times: %d".formatted(lockId, count));
        });
    }
}