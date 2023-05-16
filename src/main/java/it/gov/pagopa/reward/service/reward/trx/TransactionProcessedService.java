package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import reactor.core.publisher.Mono;

public interface TransactionProcessedService {

    Mono<TransactionDTO> checkDuplicateTransactions(TransactionDTO trx);
    Mono<BaseTransactionProcessed> save(RewardTransactionDTO trx);
}
