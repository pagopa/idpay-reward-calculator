package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedService;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InitiativesEvaluatorFacadeServiceImpl implements InitiativesEvaluatorFacadeService {

    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final InitiativesEvaluatorService initiativesEvaluatorService;
    private final UserInitiativeCountersUpdateService userInitiativeCountersUpdateService;
    private final TransactionProcessedService transactionProcessedService;
    private final Transaction2RewardTransactionMapper rewardTransactionMapper;

    public InitiativesEvaluatorFacadeServiceImpl(UserInitiativeCountersRepository userInitiativeCountersRepository, InitiativesEvaluatorService initiativesEvaluatorService, UserInitiativeCountersUpdateService userInitiativeCountersUpdateService, TransactionProcessedService transactionProcessedService, Transaction2RewardTransactionMapper rewardTransactionMapper) {
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.initiativesEvaluatorService = initiativesEvaluatorService;
        this.userInitiativeCountersUpdateService = userInitiativeCountersUpdateService;
        this.transactionProcessedService = transactionProcessedService;
        this.rewardTransactionMapper = rewardTransactionMapper;
    }

    @Override
    public Mono<RewardTransactionDTO> evaluateAndUpdateBudget(TransactionDTO trx, List<String> initiatives) {
        log.trace("[REWARD] Initiative fetched, retrieving counter: {}", trx.getId());

        final String userId = trx.getUserId();

        return userInitiativeCountersRepository.findById(userId)
                .defaultIfEmpty(new UserInitiativeCounters(userId, new HashMap<>()))
                .map(userCounters -> evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters))
                .flatMap(counters2rewardedTrx ->
                        userInitiativeCountersUpdateService.update(counters2rewardedTrx.getFirst(), counters2rewardedTrx.getSecond())
                                .then(Mono.just(counters2rewardedTrx)))
                .flatMap(counters2rewardedTrx -> {
                    RewardTransactionDTO rewardedTrx = counters2rewardedTrx.getSecond();

                    return transactionProcessedService.save(rewardedTrx)
                            .doOnNext(r -> log.trace("[REWARD] Transaction stored: {}", trx.getId()))
                            .then(userInitiativeCountersRepository.save(counters2rewardedTrx.getFirst()))
                            .doOnNext(r -> log.trace("[REWARD] Counters updated: {}", trx.getId()))
                            .then(Mono.just(rewardedTrx));
                });
    }

    @Override
    public Pair<UserInitiativeCounters, RewardTransactionDTO> evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCounters userCounters) {
        log.trace("[REWARD] Counter retrieved, evaluating initiatives: {} - {}", trx.getId(), initiatives.size());

        RewardTransactionDTO trxRewarded;
        boolean isRefund = OperationType.REFUND.equals(trx.getOperationTypeTranscoded());

        if (BigDecimal.ZERO.compareTo(trx.getEffectiveAmount()) < 0) {
            trxRewarded = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);
            if (isRefund) {
                handlePartialRefund(trxRewarded);
            }
        } else {
            trxRewarded = rewardTransactionMapper.apply(trx);
            if (isRefund) {
                handleCompleteRefund(trx, trxRewarded);
            } else {
                trxRewarded.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_INVALID_AMOUNT));
                trxRewarded.setStatus(RewardConstants.REWARD_STATE_REJECTED);
            }
        }
        return Pair.of(userCounters, trxRewarded);
    }

    private void handleCompleteRefund(TransactionDTO trx, RewardTransactionDTO trxRewarded) {
        BaseTransactionProcessed lastTrx = trx.getRefundInfo()==null || CollectionUtils.isEmpty(trx.getRefundInfo().getPreviousTrxs())
                ? null
                : trx.getRefundInfo().getPreviousTrxs().get(trx.getRefundInfo().getPreviousTrxs().size() - 1);

        trxRewarded.setInitiativeRejectionReasons(lastTrx==null
                ? Collections.emptyMap()
                : lastTrx.getInitiativeRejectionReasons());

        if (trx.getRefundInfo() == null || trx.getRefundInfo().getPreviousRewards().size() == 0) {
            
            trxRewarded.setRejectionReasons(lastTrx==null
                    ? List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE)
                    : lastTrx.getRejectionReasons());

            trxRewarded.setStatus(RewardConstants.REWARD_STATE_REJECTED);
        } else {
            trxRewarded.setRewards(trx.getRefundInfo().getPreviousRewards().entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), new Reward(e.getKey(), e.getValue().getOrganizationId(), e.getValue().getAccruedReward().negate(), true)))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));

            boolean isRejected = trx.getRefundInfo().getPreviousRewards().values().stream().noneMatch(r -> BigDecimal.ZERO.compareTo(r.getAccruedReward()) != 0);

            if(isRejected) {
                trxRewarded.setRejectionReasons(lastTrx == null
                        ? Collections.emptyList()
                        : lastTrx.getRejectionReasons());
            }

            trxRewarded.setStatus(
                    isRejected
                    ? RewardConstants.REWARD_STATE_REJECTED
                    : RewardConstants.REWARD_STATE_REWARDED);
        }
    }

    private void handlePartialRefund(RewardTransactionDTO trxRewarded) {
        Map<String, RefundInfo.PreviousReward> pastRewards = new HashMap<>(trxRewarded.getRefundInfo() != null ? trxRewarded.getRefundInfo().getPreviousRewards() : Collections.emptyMap());
        trxRewarded.getRewards().forEach((initiativeId, r)-> {
            RefundInfo.PreviousReward pastReward = pastRewards.remove(initiativeId);
            if(pastReward!=null){
                r.setAccruedReward(r.getAccruedReward().subtract(pastReward.getAccruedReward()));
            }
            r.setRefund(true);
        });
        pastRewards.forEach((initiativeId, reward2Reverse) -> trxRewarded.getRewards().put(initiativeId, new Reward(initiativeId, reward2Reverse.getOrganizationId(), reward2Reverse.getAccruedReward().negate(), true)));
        if(trxRewarded.getRewards().values().stream().anyMatch(r -> BigDecimal.ZERO.compareTo(r.getAccruedReward()) != 0)){
            trxRewarded.setStatus(RewardConstants.REWARD_STATE_REWARDED);
        } else {
            trxRewarded.setInitiativeRejectionReasons(
                    trxRewarded.getRefundInfo() != null && !CollectionUtils.isEmpty(trxRewarded.getRefundInfo().getPreviousTrxs())
                            ? trxRewarded.getRefundInfo().getPreviousTrxs().get(0).getInitiativeRejectionReasons()
                            : Collections.emptyMap());
        }
    }
}
