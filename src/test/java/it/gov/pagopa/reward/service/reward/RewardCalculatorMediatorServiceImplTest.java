package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.ReversalInfo;
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
        TransactionDTO trxCompleteReverseNoCharge = buildTrx(2);
        TransactionDTO trxCompleteReverse = buildTrx(3);
        TransactionDTO trxPartialReverse = buildTrx(4);
        Flux<Message<String>> trxFlux = buildTrxFlux(trx, trxInvalidOpType, trxCompleteReverseNoCharge, trxCompleteReverse, trxPartialReverse);

        List<String> initiatives = List.of("INITIATIVE");
        Mockito.when(onboardedInitiativesServiceMock.getInitiatives(Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromIterable(initiatives));

        mockReversalUseCases(trxInvalidOpType, trxCompleteReverseNoCharge, trxCompleteReverse, trxPartialReverse);

        Mockito.when(initiativesEvaluatorServiceMock.evaluateInitiativesBudgetAndRules(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(i-> rewardTransactionMapper.apply(i.getArgument(0)));

        // When
        List<RewardTransactionDTO> result = rewardCalculatorMediatorService.execute(trxFlux).collectList().block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.size());

        Assertions.assertEquals(List.of("REJECTED"), result.get(1).getRejectionReasons());
        Assertions.assertEquals(List.of("NO_ACTIVE_INITIATIVES"), result.get(2).getRejectionReasons());

        Mockito.verify(operationTypeHandlerServiceMock).handleOperationType(trx);
        Mockito.verify(operationTypeHandlerServiceMock).handleOperationType(trxInvalidOpType);
        Mockito.verify(operationTypeHandlerServiceMock).handleOperationType(trxCompleteReverseNoCharge);
        Mockito.verify(operationTypeHandlerServiceMock).handleOperationType(trxCompleteReverse);
        Mockito.verify(operationTypeHandlerServiceMock).handleOperationType(trxPartialReverse);

        Mockito.verify(onboardedInitiativesServiceMock).getInitiatives(trx.getHpan(), trx.getTrxDate());
        Mockito.verify(onboardedInitiativesServiceMock).getInitiatives(trxPartialReverse.getHpan(), trxPartialReverse.getTrxDate());

        Mockito.verify(userInitiativeCountersRepositoryMock).findById(trx.getUserId());
        Mockito.verify(userInitiativeCountersRepositoryMock).findById(trxPartialReverse.getUserId());

        Mockito.verify(initiativesEvaluatorServiceMock).evaluateInitiativesBudgetAndRules(Mockito.eq(trx), Mockito.eq(initiatives), Mockito.any());
        Mockito.verify(initiativesEvaluatorServiceMock).evaluateInitiativesBudgetAndRules(Mockito.eq(trxPartialReverse), Mockito.eq(initiatives), Mockito.any());
        Mockito.verify(initiativesEvaluatorServiceMock).evaluateInitiativesBudgetAndRules(Mockito.eq(trxCompleteReverse), Mockito.eq(List.of("INITIATIVE2REVERSE")), Mockito.any());

        Mockito.verify(userInitiativeCountersRepositoryMock).save(Mockito.argThat(i->i.getUserId().equals(trx.getUserId())));
        Mockito.verify(userInitiativeCountersRepositoryMock).save(Mockito.argThat(i->i.getUserId().equals(trxPartialReverse.getUserId())));
        Mockito.verify(userInitiativeCountersRepositoryMock).save(Mockito.argThat(i->i.getUserId().equals(trxCompleteReverse.getUserId())));

        Mockito.verifyNoMoreInteractions(operationTypeHandlerServiceMock, onboardedInitiativesServiceMock, userInitiativeCountersRepositoryMock, initiativesEvaluatorServiceMock);

        Mockito.verifyNoInteractions(errorNotifierServiceMock);

    }

    private TransactionDTO buildTrx(int i) {
        final TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
        trx.setAmount(BigDecimal.valueOf(i+1));
        return trx;
    }

    private void mockReversalUseCases(TransactionDTO trxInvalidOpType, TransactionDTO trxCompleteReverseNoCharge, TransactionDTO trxCompleteReverse, TransactionDTO trxPartialReverse) {
        Mockito.when(operationTypeHandlerServiceMock.handleOperationType(trxInvalidOpType)).thenAnswer(i-> {
            final TransactionDTO t = i.getArgument(0);
            t.setRejectionReasons(List.of("REJECTED"));
            return Mono.just(t);
        });

        Mockito.when(operationTypeHandlerServiceMock.handleOperationType(trxCompleteReverseNoCharge)).thenAnswer(i-> {
            final TransactionDTO t = i.getArgument(0);
            t.setOperationTypeTranscoded(OperationType.REVERSAL);
            t.setEffectiveAmount(BigDecimal.ZERO);
            return Mono.just(t);
        });

        Mockito.when(operationTypeHandlerServiceMock.handleOperationType(trxCompleteReverse)).thenAnswer(i-> {
            final TransactionDTO t = i.getArgument(0);
            t.setOperationTypeTranscoded(OperationType.REVERSAL);
            t.setEffectiveAmount(BigDecimal.ZERO);
            t.setReversalInfo(new ReversalInfo());
            t.getReversalInfo().setPreviousRewards(Map.of("INITIATIVE2REVERSE", new Reward()));
            return Mono.just(t);
        });

        Mockito.when(operationTypeHandlerServiceMock.handleOperationType(trxPartialReverse)).thenAnswer(i-> {
            final TransactionDTO t = i.getArgument(0);
            t.setOperationTypeTranscoded(OperationType.REVERSAL);
            t.setEffectiveAmount(t.getAmount());
            t.setReversalInfo(new ReversalInfo());
            t.getReversalInfo().setPreviousRewards(Map.of("INITIATIVE2REVERSE", new Reward()));
            return Mono.just(t);
        });
    }

}