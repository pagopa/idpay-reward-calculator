package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.ErrorNotifierService;
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
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@ExtendWith(MockitoExtension.class)
class RewardCalculatorMediatorServiceImplTest {

    @BeforeAll
    public static void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Rome")));
    }

    @Mock private OperationTypeHandlerService operationTypeHandlerServiceMock;
    @Mock private TransactionValidatorService transactionValidatorServiceMock;
    @Mock private OnboardedInitiativesService onboardedInitiativesServiceMock;
    @Mock private UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;
    @Mock private UserInitiativeCountersUpdateService userInitiativeCountersUpdateServiceMock;
    @Mock private InitiativesEvaluatorService initiativesEvaluatorServiceMock;
    @Mock private ErrorNotifierService errorNotifierServiceMock;

    private RewardCalculatorMediatorService rewardCalculatorMediatorService;

    private final Transaction2RewardTransactionMapper rewardTransactionMapper = new Transaction2RewardTransactionMapper();

    @BeforeEach
    public void initMocks(){
        rewardCalculatorMediatorService = new RewardCalculatorMediatorServiceImpl(
                operationTypeHandlerServiceMock,
                transactionValidatorServiceMock,
                onboardedInitiativesServiceMock,
                userInitiativeCountersRepositoryMock,
                initiativesEvaluatorServiceMock,
                userInitiativeCountersUpdateServiceMock,
                rewardTransactionMapper,
                errorNotifierServiceMock,
                TestUtils.objectMapper);

        Mockito.when(operationTypeHandlerServiceMock.handleOperationType(Mockito.any())).thenAnswer(i-> {
            final TransactionDTO trx = i.getArgument(0);
            trx.setOperationTypeTranscoded(OperationType.CHARGE);
            trx.setTrxChargeDate(trx.getTrxDate());
            trx.setEffectiveAmount(trx.getAmount());
            return Mono.just(trx);
        });

        Mockito.when(transactionValidatorServiceMock.validate(Mockito.any())).thenAnswer(i->i.getArgument(0));

        Mockito.when(userInitiativeCountersRepositoryMock.findById(Mockito.<String>any())).thenReturn(Mono.empty());
        Mockito.when(userInitiativeCountersRepositoryMock.save(Mockito.any())).thenReturn(Mono.empty());
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
        TransactionDTO trxTotalRefundNoCharge = buildTrx(5);
        Flux<Message<String>> trxFlux = buildTrxFlux(trx, trxInvalidOpType, trxInvalidAmount, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);

        List<String> expectedInitiatives = List.of("INITIATIVE");

        mockUseCases(expectedInitiatives, trxInvalidOpType, trxInvalidAmount, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);

        // When
        List<RewardTransactionDTO> result = rewardCalculatorMediatorService.execute(trxFlux).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(6, result.size());

        assertRejectionReasons(result, 1, "REJECTED");
        assertRejectionReasons(result, 2, "INVALID_AMOUNT");
        assertRejectionReasons(result, 4,null);
        assertRejectionReasons(result, 5, "NO_ACTIVE_INITIATIVES");

        verifyOperationTypeCalls(trx, trxInvalidOpType, trxInvalidAmount, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);
        verifyOnboardedInitiativeCalls(trx, trxPartialRefund);
        verifyUserInitiativeCounterFindByIdCalls(trx, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);
        verifyInitiativeEvaluatorCalls(expectedInitiatives,trx, trxPartialRefund);
        verifyUserInitiativeCounterSaveCalls(trx, trxPartialRefund, trxTotalRefund);

        Mockito.verifyNoMoreInteractions(operationTypeHandlerServiceMock, onboardedInitiativesServiceMock, userInitiativeCountersRepositoryMock, initiativesEvaluatorServiceMock);

        Mockito.verifyNoInteractions(errorNotifierServiceMock);

    }

    private void assertRejectionReasons(List<RewardTransactionDTO> result, int index, String expectedRejectionReason) {
        Assertions.assertEquals(expectedRejectionReason != null ? List.of(expectedRejectionReason) : Collections.emptyList(), result.get(index).getRejectionReasons());
        Assertions.assertEquals(expectedRejectionReason != null ? "REJECTED" : "REWARDED", result.get(index).getStatus());
    }

    private TransactionDTO buildTrx(int i) {
        final TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
        trx.setAmount(BigDecimal.valueOf(i+1));
        return trx;
    }

    private void mockUseCases(List<String> initiatives, TransactionDTO trxInvalidOpType, TransactionDTO trxInvalidAmount, TransactionDTO trxPartialReverse, TransactionDTO trxTotalRefund, TransactionDTO trxTotalRefundNoCharge) {
        Mockito.when(onboardedInitiativesServiceMock.getInitiatives(Mockito.any()))
                .thenReturn(Flux.fromIterable(initiatives));

        Mockito.when(initiativesEvaluatorServiceMock.evaluateInitiativesBudgetAndRules(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(i-> rewardTransactionMapper.apply(i.getArgument(0)));

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

        Mockito.when(operationTypeHandlerServiceMock.handleOperationType(trxTotalRefundNoCharge)).thenAnswer(i-> {
            final TransactionDTO t = i.getArgument(0);
            t.setOperationTypeTranscoded(OperationType.REFUND);
            t.setEffectiveAmount(BigDecimal.ZERO);
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

    private void verifyUserInitiativeCounterSaveCalls(TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(userInitiativeCountersRepositoryMock).save(Mockito.argThat(i->i.getUserId().equals(t.getUserId())));
        }
    }
}