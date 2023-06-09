package it.gov.pagopa.reward.service.synchronous.op.recover;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import reactor.core.publisher.Mono;

public interface HandleSyncCounterUpdatingTrxService {
    Mono<UserInitiativeCounters> checkUpdatingTrx(TransactionDTO trxDTO, UserInitiativeCounters counters);
}
