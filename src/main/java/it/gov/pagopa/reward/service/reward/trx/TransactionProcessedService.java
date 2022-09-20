package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import reactor.core.publisher.Mono;

public interface TransactionProcessedService {

    Mono<TransactionDTO> checkDuplicateTransactions(TransactionDTO trx);
    Mono<TransactionProcessed> save(RewardTransactionDTO trx);
}
