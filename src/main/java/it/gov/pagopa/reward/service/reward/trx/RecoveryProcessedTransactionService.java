package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import reactor.core.publisher.Mono;

public interface RecoveryProcessedTransactionService {
    Mono<Void> checkIf2Recover(TransactionDTO trx, TransactionProcessed trxStored);
}
