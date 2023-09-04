package it.gov.pagopa.reward.connector.repository;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.model.TransactionProcessed;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionProcessedAtomicOpsRepository {
    Mono<DeleteResult> removeByInitiativeId(String initiativeId);
    Mono<UpdateResult> removeInitiativeOnTransaction(String initiativeId);
    Flux<TransactionProcessed> deleteTransactionsWithoutInitiative();
}
