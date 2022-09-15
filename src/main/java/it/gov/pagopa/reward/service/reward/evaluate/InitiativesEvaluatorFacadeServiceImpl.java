package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.LockService;
import it.gov.pagopa.reward.service.reward.TransactionProcessedService;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InitiativesEvaluatorFacadeServiceImpl implements InitiativesEvaluatorFacadeService {

    private final LockService lockService;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final InitiativesEvaluatorService initiativesEvaluatorService;
    private final UserInitiativeCountersUpdateService userInitiativeCountersUpdateService;
    private final TransactionProcessedService transactionProcessedService;
    private final Transaction2RewardTransactionMapper rewardTransactionMapper;

    public InitiativesEvaluatorFacadeServiceImpl(LockService lockService, UserInitiativeCountersRepository userInitiativeCountersRepository, InitiativesEvaluatorService initiativesEvaluatorService, UserInitiativeCountersUpdateService userInitiativeCountersUpdateService, TransactionProcessedService transactionProcessedService, Transaction2RewardTransactionMapper rewardTransactionMapper) {
        this.lockService = lockService;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
        this.initiativesEvaluatorService = initiativesEvaluatorService;
        this.userInitiativeCountersUpdateService = userInitiativeCountersUpdateService;
        this.transactionProcessedService = transactionProcessedService;
        this.rewardTransactionMapper = rewardTransactionMapper;
    }

    @Override
    public Mono<RewardTransactionDTO> evaluate(TransactionDTO trx, List<String> initiatives) {
        final String userId = trx.getUserId();
        final int lockId = calculateLockId(userId);
        lockService.acquireLock(lockId);
        log.debug("[REWARD] [LOCK_ACQUIRED] trx having userId {} acquired lock having id {}", userId, lockId);
        return userInitiativeCountersRepository.findById(userId)
                .defaultIfEmpty(new UserInitiativeCounters(userId, new HashMap<>()))
                .map(userCounters -> evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters))
                .flatMap(counters2rewardedTrx -> {
                    RewardTransactionDTO rewardedTrx = counters2rewardedTrx.getSecond();
                    userInitiativeCountersUpdateService.update(counters2rewardedTrx.getFirst(), rewardedTrx);

                    return transactionProcessedService.save(rewardedTrx)
                            .then(userInitiativeCountersRepository.save(counters2rewardedTrx.getFirst()))
                            .then(Mono.just(rewardedTrx));
                })
                .doFinally(t-> {
                    lockService.releaseLock(lockId);
                    log.debug("[REWARD] [LOCK_RELEASED] trx having userId {} released lock having id {}", userId, lockId);
                });
    }

    public int calculateLockId(String userId){
        return Math.abs(userId.hashCode()) % lockService.getBuketSize();
    }

    private Pair<UserInitiativeCounters, RewardTransactionDTO> evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCounters userCounters) {
        RewardTransactionDTO trxRewarded;
        if(BigDecimal.ZERO.compareTo(trx.getEffectiveAmount()) < 0){
            trxRewarded = initiativesEvaluatorService.evaluateInitiativesBudgetAndRules(trx, initiatives, userCounters);
        } else {
            trxRewarded = rewardTransactionMapper.apply(trx);
            if(OperationType.REFUND.equals(trx.getOperationTypeTranscoded())){
                handleCompleteRefund(trx, trxRewarded);
            } else {
                trxRewarded.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_INVALID_AMOUNT));
                trxRewarded.setStatus(RewardConstants.REWARD_STATE_REJECTED);
            }
        }
        return Pair.of(userCounters, trxRewarded);
    }

    private void handleCompleteRefund(TransactionDTO trx, RewardTransactionDTO trxRewarded) {
        if(trx.getRefundInfo() == null || trx.getRefundInfo().getPreviousRewards().size() == 0){
            trxRewarded.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE));
            trxRewarded.setStatus(RewardConstants.REWARD_STATE_REJECTED);
        } else {
            trxRewarded.setRewards(trx.getRefundInfo().getPreviousRewards().entrySet().stream()
                    .map(e->Pair.of(e.getKey(), new Reward(e.getValue().negate())))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
            trxRewarded.setStatus(RewardConstants.REWARD_STATE_REWARDED);
        }
    }
}
