package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.StreamSupport;

@ExtendWith(MockitoExtension.class)
class InitiativeEvaluatorFacadeServiceTest {

    @BeforeAll
    public static void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(CommonConstants.ZONEID));
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
    private RewardErrorNotifierService rewardErrorNotifierServiceMock;

    private InitiativesEvaluatorFacadeServiceImpl initiativesEvaluatorFacadeService;

    private final Transaction2RewardTransactionMapper rewardTransactionMapper = new Transaction2RewardTransactionMapper();
    private final Transaction2TransactionProcessedMapper reward2ProcessedMapper = new Transaction2TransactionProcessedMapper();

    @BeforeEach
    public void initMocks() {
        initiativesEvaluatorFacadeService = new InitiativesEvaluatorFacadeServiceImpl(
                3, 1, userInitiativeCountersRepositoryMock,
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

        invalidTrx.setEffectiveAmountCents(CommonUtilities.euroToCents(invalidTrx.getAmount().negate()));

        List<String> initiatives = List.of("INITIATIVE");

        mockUseCases(trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);

        // When
        @SuppressWarnings("ConstantConditions") List<RewardTransactionDTO> result = trxs.stream().map(t -> initiativesEvaluatorFacadeService.evaluateAndUpdateBudget(t, initiatives).block()).toList();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(trxs.size(), result.size());

        assertRejectionReasons(result, trx, null);
        assertRejectionReasons(result, invalidTrx, "INVALID_AMOUNT");
        assertRejectionReasons(result, trxPartialRefund, null);
        assertRejectionReasons(result, trxTotalRefund, null);
        assertRejectionReasons(result, trxTotalRefundNoCharge, "NO_ACTIVE_INITIATIVES");

        verifyUserInitiativeCounterFindByIdCalls(initiatives, trx, trxPartialRefund, trxTotalRefund, trxTotalRefundNoCharge);
        verifyInitiativeEvaluatorCalls(initiatives, trx, trxPartialRefund);
        verifyTransactionProcessedSaveCalls(trx);
        verifyUserInitiativeCounterSaveCalls(trx, trxPartialRefund, trxTotalRefund);

        Mockito.verifyNoMoreInteractions(transactionProcessedService, userInitiativeCountersRepositoryMock, initiativesEvaluatorServiceMock);

        Mockito.verifyNoInteractions(rewardErrorNotifierServiceMock);

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
        trx.setUserId("USERID_" + i);
        trx.setAmount(BigDecimal.valueOf(i + 1));
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        trx.setTrxChargeDate(trx.getTrxDate());
        trx.setEffectiveAmountCents(CommonUtilities.euroToCents(trx.getAmount()));
        return trx;
    }

    private void mockUseCases(TransactionDTO trxPartialReverse, TransactionDTO trxTotalRefund, TransactionDTO trxTotalRefundNoCharge) {
        Mockito.when(transactionProcessedService.save(Mockito.any())).thenAnswer(i -> Mono.just(reward2ProcessedMapper.apply(i.getArgument(0))));

        Mockito.when(userInitiativeCountersRepositoryMock.findByEntityIdAndInitiativeIdIn(Mockito.any(), Mockito.any())).thenReturn(Flux.empty());
        Mockito.when(userInitiativeCountersRepositoryMock.saveAll(Mockito.<Iterable<UserInitiativeCounters>>any())).thenReturn(Flux.empty());

        Mockito.when(initiativesEvaluatorServiceMock.evaluateInitiativesBudgetAndRules(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(i -> rewardTransactionMapper.apply(i.getArgument(0)));

        Mockito.when(userInitiativeCountersUpdateServiceMock.update(Mockito.any(),Mockito.any())).thenAnswer(i-> {
            UserInitiativeCountersWrapper counters = i.getArgument(0, UserInitiativeCountersWrapper.class);
            counters.setInitiatives(Map.of("INITIATIVE", new UserInitiativeCounters(counters.getEntityId(), InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,"INITIATIVE")));
            return Mono.just(i.getArgument(1));
        });

        Mockito.when(initiativesEvaluatorServiceMock.evaluateInitiativesBudgetAndRules(Mockito.eq(trxPartialReverse), Mockito.any(), Mockito.any()))
                .thenAnswer(i -> {
                    RewardTransactionDTO reward = rewardTransactionMapper.apply(i.getArgument(0));
                    reward.setRewards(new HashMap<>(Map.of("INITIATIVE2PARTIALREVERSE", new Reward("INITIATIVE2PARTIALREVERSE", "ORGANIZATION", 9_00L))));
                    reward.setInitiativeRejectionReasons(new HashMap<>(Map.of("INITIATIVE2REVERSE", List.of("NOT_MORE_REWARDED_FOR_SOME_REASON"))));
                    return reward;
                });

        trxPartialReverse.setOperationTypeTranscoded(OperationType.REFUND);
        trxPartialReverse.setEffectiveAmountCents(CommonUtilities.euroToCents(trxPartialReverse.getAmount()));
        trxPartialReverse.setRefundInfo(new RefundInfo());
        trxPartialReverse.getRefundInfo().setPreviousRewards(Map.of(
                "INITIATIVE2PARTIALREVERSE", new RefundInfo.PreviousReward("INITIATIVE2PARTIALREVERSE", "ORGANIZATION", 10_00L),
                "INITIATIVE2REVERSE", new RefundInfo.PreviousReward("INITIATIVE2REVERSE", "ORGANIZATION", 1_00L)));

        trxTotalRefundNoCharge.setOperationTypeTranscoded(OperationType.REFUND);
        trxTotalRefundNoCharge.setEffectiveAmountCents(0L);

        trxTotalRefund.setOperationTypeTranscoded(OperationType.REFUND);
        trxTotalRefund.setEffectiveAmountCents(0L);
        trxTotalRefund.setRefundInfo(new RefundInfo());
        trxTotalRefund.getRefundInfo().setPreviousRewards(Map.of("INITIATIVE2REVERSE", new RefundInfo.PreviousReward("INITIATIVE2REVERSE", "ORGANIZATION", 1_00L)));
    }

    private void verifyUserInitiativeCounterFindByIdCalls(List<String> initiativeIds, TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(userInitiativeCountersRepositoryMock).findByEntityIdAndInitiativeIdIn(t.getUserId(), initiativeIds);
        }
    }

    private void verifyInitiativeEvaluatorCalls(List<String> expectedInitiatives, TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(initiativesEvaluatorServiceMock).evaluateInitiativesBudgetAndRules(Mockito.eq(t), Mockito.eq(expectedInitiatives), Mockito.any());
        }
    }

    private void verifyTransactionProcessedSaveCalls(TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(transactionProcessedService).save(Mockito.argThat(o -> {
                RewardTransactionDTO arg = rewardTransactionMapper.apply(t);
                arg.setElaborationDateTime(o.getElaborationDateTime());
                return o.equals(arg);
            }));
        }
    }

    private void verifyUserInitiativeCounterSaveCalls(TransactionDTO... expectedTrxs) {
        for (TransactionDTO t : expectedTrxs) {
            Mockito.verify(userInitiativeCountersRepositoryMock).saveAll(Mockito.<Iterable<UserInitiativeCounters>>argThat(i -> i.iterator().hasNext() && StreamSupport.stream(i.spliterator(), false).allMatch(c -> c.getEntityId().equals(t.getUserId()))));
        }
    }

    private void checkPartialRefundResult(RewardTransactionDTO rewardTransactionDTO) {
        Assertions.assertEquals(new Reward("INITIATIVE2PARTIALREVERSE", "ORGANIZATION", 9_00L, -1_00L, false, true), rewardTransactionDTO.getRewards().get("INITIATIVE2PARTIALREVERSE"));
        Assertions.assertEquals(new Reward("INITIATIVE2REVERSE", "ORGANIZATION", -1_00L, true), rewardTransactionDTO.getRewards().get("INITIATIVE2REVERSE"));
        Assertions.assertEquals(Map.of("INITIATIVE2REVERSE", List.of("NOT_MORE_REWARDED_FOR_SOME_REASON")), rewardTransactionDTO.getInitiativeRejectionReasons());
    }

    private void checkTotalRefundResult(RewardTransactionDTO rewardTransactionDTO) {
        Assertions.assertEquals(new Reward("INITIATIVE2REVERSE", "ORGANIZATION",-1_00L, true), rewardTransactionDTO.getRewards().get("INITIATIVE2REVERSE"));
    }
}