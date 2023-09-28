package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.model.TransactionProcessed;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionProcessedAtomicOpsRepository {
    Mono<Void> removeInitiativeOnTransaction(String trxId, String initiativeId);
    Flux<TransactionProcessed> deleteTransactionsWithoutInitiative();
    Flux<TransactionProcessed> findByInitiativesWithBatch(String initiativeId, int batchSize);
    Flux<TransactionProcessed> findWithoutInitiativesWithBatch(int batchSize);
}
