package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.mapper.trx.recover.RecoveredTrx2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.trx.recover.RecoveredTrx2UserInitiativeCountersMapper;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.TransactionRepository;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.RewardNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class RecoveryProcessedTransactionServiceImpl implements RecoveryProcessedTransactionService {

    private final UserInitiativeCountersRepository countersRepository;
    private final RecoveredTrx2RewardTransactionMapper recoveredTrx2RewardTransactionMapper;
    private final RecoveredTrx2UserInitiativeCountersMapper recoveredTrx2UserInitiativeCountersMapper;
    private final RewardNotifierService rewardNotifierService;
    private final TransactionRepository transactionRepository;

    public RecoveryProcessedTransactionServiceImpl(UserInitiativeCountersRepository countersRepository, RecoveredTrx2RewardTransactionMapper recoveredTrx2RewardTransactionMapper, RecoveredTrx2UserInitiativeCountersMapper recoveredTrx2UserInitiativeCountersMapper, RewardNotifierService rewardNotifierService, TransactionRepository transactionRepository) {
        this.countersRepository = countersRepository;
        this.recoveredTrx2RewardTransactionMapper = recoveredTrx2RewardTransactionMapper;
        this.recoveredTrx2UserInitiativeCountersMapper = recoveredTrx2UserInitiativeCountersMapper;
        this.rewardNotifierService = rewardNotifierService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Mono<Void> checkIf2Recover(TransactionDTO trx, TransactionProcessed trxStored) {
        if (!CollectionUtils.isEmpty(trxStored.getRewards())) {
            return countersRepository.findByUserIdAndInitiativeIdIn(trxStored.getUserId(), trxStored.getRewards().keySet())
                    .collectMap(UserInitiativeCounters::getInitiativeId)
                    .flatMap(storedCounters -> compareCounters(trx, trxStored, storedCounters));
        } else {
            return publishTrxIfNotEmpty(trx, trxStored, checkIf2PublishWhenNotCountersUpdated(trx, trxStored))
                    .then();
        }
    }

    private Mono<Void> compareCounters(TransactionDTO trx, TransactionProcessed trxStored, Map<String, UserInitiativeCounters> storedCounters) {
        List<UserInitiativeCounters> countersToStore = trxStored.getRewards().values()
                .stream()
                .map(r -> {
                    UserInitiativeCounters sc = storedCounters.get(r.getInitiativeId());
                    if(sc == null || sc.getVersion() < r.getCounters().getVersion()){
                        return recoveredTrx2UserInitiativeCountersMapper.apply(r, trxStored, sc);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        Mono<?> mono;

        if (!CollectionUtils.isEmpty(countersToStore)) {
            log.info("[REWARD][RECOVERY_TRX] Found recovered transaction with updated counters {}", trxStored.getId());
            mono = countersRepository.saveAll(countersToStore).collectList();
        } else {
            mono = checkIf2PublishWhenNotCountersUpdated(trx, trxStored);
        }

        return publishTrxIfNotEmpty(trx, trxStored, mono)
                .then();
    }

    private Mono<?> checkIf2PublishWhenNotCountersUpdated(TransactionDTO trx, TransactionProcessed trxStored){
        // only if we are reading again the same message we should notify it when not counters 2 update are involved
        // otherwise we could assume that the original message has been published
        if(trxStored.getRuleEngineTopicOffset().equals(trx.getRuleEngineTopicOffset())){
            return checkIfNotExistsIntoTransactionCollection(trxStored);
        } else {
            return Mono.empty();
        }
    }

    private Mono<?> checkIfNotExistsIntoTransactionCollection(TransactionProcessed trxStored) {
        return transactionRepository.checkIfExists(trxStored.getId())
                .mapNotNull(r -> Boolean.TRUE.equals(r)? null : false);
    }

    private Mono<?> publishTrxIfNotEmpty(TransactionDTO trx, TransactionProcessed trxStored, Mono<?> mono) {
        return mono.doOnNext(x -> {
            RewardTransactionDTO rewardedTrx = recoveredTrx2RewardTransactionMapper.apply(trx, trxStored);
            rewardNotifierService.notifyFallbackToErrorTopic(rewardedTrx);
        });
    }
}
