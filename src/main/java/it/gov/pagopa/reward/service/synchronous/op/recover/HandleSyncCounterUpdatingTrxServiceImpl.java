package it.gov.pagopa.reward.service.synchronous.op.recover;

import it.gov.pagopa.reward.connector.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class HandleSyncCounterUpdatingTrxServiceImpl implements HandleSyncCounterUpdatingTrxService {

    private final TransactionProcessedRepository transactionProcessedRepository;
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;

    public HandleSyncCounterUpdatingTrxServiceImpl(TransactionProcessedRepository transactionProcessedRepository, UserInitiativeCountersRepository userInitiativeCountersRepository) {
        this.transactionProcessedRepository = transactionProcessedRepository;
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;
    }

    @Override
    public Mono<UserInitiativeCounters> checkUpdatingTrx(TransactionDTO trxDTO, UserInitiativeCounters counters) {
        if(List.of(trxDTO.getId()).equals(counters.getUpdatingTrxId())){
            emptyUpdatingTrxId(counters);
            return Mono.just(counters);
        } else {
            log.info("[SYNC_TRANSACTION] Found stuck transaction {} while handling new trx {} (counterId:{}), removing it", counters.getUpdatingTrxId(), trxDTO.getId(), counters.getId());

            Mono<Void> deleteAllIdsMono;
            if(!CollectionUtils.isEmpty(counters.getUpdatingTrxId())){
                deleteAllIdsMono = transactionProcessedRepository.deleteAllById(counters.getUpdatingTrxId());
            } else {
                deleteAllIdsMono = Mono.empty();
            }

            return deleteAllIdsMono
                    .then(userInitiativeCountersRepository.setUpdatingTrx(counters.getId(), trxDTO.getId()))
                    .doOnNext(this::emptyUpdatingTrxId);
        }
    }

    private void emptyUpdatingTrxId(UserInitiativeCounters counters) {
        counters.setUpdatingTrxId(null);
    }
}
