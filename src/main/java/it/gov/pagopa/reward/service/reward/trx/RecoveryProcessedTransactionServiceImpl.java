package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.mapper.trx.recover.RecoveredTrx2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.TransactionRepository;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.RewardNotifierService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class RecoveryProcessedTransactionServiceImpl implements RecoveryProcessedTransactionService {

    private final UserInitiativeCountersRepository countersRepository;
    private final RecoveredTrx2RewardTransactionMapper recoveredTrx2RewardTransactionMapper;
    private final RewardNotifierService rewardNotifierService;
    private final TransactionRepository transactionRepository;

    public RecoveryProcessedTransactionServiceImpl(UserInitiativeCountersRepository countersRepository, RecoveredTrx2RewardTransactionMapper recoveredTrx2RewardTransactionMapper, RewardNotifierService rewardNotifierService, TransactionRepository transactionRepository) {
        this.countersRepository = countersRepository;
        this.recoveredTrx2RewardTransactionMapper = recoveredTrx2RewardTransactionMapper;
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
            return publishTrxIfNotEmpty(trx, trxStored, checkIfNotExistsIntoTransactionCollection(trxStored))
                    .then();
        }
    }

    private Mono<Void> compareCounters(TransactionDTO trx, TransactionProcessed trxStored, Map<String, UserInitiativeCounters> storedCounters) {
        List<UserInitiativeCounters> countersToStore = trxStored.getRewards().values()
                .stream()
                .filter(r -> {
                    UserInitiativeCounters sc = storedCounters.get(r.getInitiativeId());
                    return sc == null || sc == null; //TODO compare version
                })
                .map(r -> (UserInitiativeCounters)null)
                .toList();

        Mono<?> mono;

        if (!CollectionUtils.isEmpty(countersToStore)) {
            mono = countersRepository.saveAll(countersToStore).collectList();
        } else {
            mono = checkIfNotExistsIntoTransactionCollection(trxStored);
        }

        return publishTrxIfNotEmpty(trx, trxStored, mono)
                .then();
    }

    private Mono<?> checkIfNotExistsIntoTransactionCollection(TransactionProcessed trxStored) {
        return transactionRepository.checkIfExists(trxStored.getId())
                .mapNotNull(r -> Boolean.TRUE.equals(r)? true : null);
    }

    private Mono<?> publishTrxIfNotEmpty(TransactionDTO trx, TransactionProcessed trxStored, Mono<?> mono) {
        return mono.doOnNext(x -> {
            RewardTransactionDTO rewardedTrx = recoveredTrx2RewardTransactionMapper.apply(trx, trxStored);
            rewardNotifierService.notify(rewardedTrx);
        });
    }
}
