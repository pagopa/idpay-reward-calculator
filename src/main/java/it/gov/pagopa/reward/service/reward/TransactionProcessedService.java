package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import reactor.core.publisher.Mono;

public interface TransactionProcessedService {

    Mono<TransactionProcessed> getProcessedTransactions(String trxId);
    Mono<TransactionProcessed> save(RewardTransactionDTO trx);
}
