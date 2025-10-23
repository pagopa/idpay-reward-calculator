package it.gov.pagopa.reward.connector.repository.primary;

import it.gov.pagopa.reward.model.TransactionProcessed;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TransactionProcessedAtomicOpsRepositoryImpl implements TransactionProcessedAtomicOpsRepository {
    private final ReactiveMongoTemplate mongoTemplate;

    public TransactionProcessedAtomicOpsRepositoryImpl(@Qualifier("reactiveMongoTemplate") ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Void> removeInitiativeOnTransaction(String trxId, String initiativeId) {
        return mongoTemplate
                .updateFirst(
                        Query.query(Criteria.where(TransactionProcessed.Fields.id).is(trxId)),
                        new Update()
                                .pull(TransactionProcessed.Fields.initiatives, initiativeId)
                                .unset("%s.%s".formatted(TransactionProcessed.Fields.rewards, initiativeId))
                                .unset("%s.%s".formatted(TransactionProcessed.Fields.initiativeRejectionReasons, initiativeId)),
                        TransactionProcessed.class
                ).then();
    }

    @Override
    public Flux<TransactionProcessed> deleteTransactionsWithoutInitiative() {
        return mongoTemplate
                .findAllAndRemove(
                        Query.query(
                                Criteria.where(TransactionProcessed.Fields.initiatives)
                                        .size(0)),
                        TransactionProcessed.class
                );
    }

    @Override
    public Flux<TransactionProcessed> findByInitiativesWithBatch(String initiativeId, int batchSize){
        Query query = Query.query(Criteria.where(TransactionProcessed.Fields.initiatives).is(initiativeId)).cursorBatchSize(batchSize);
        return mongoTemplate.find(query, TransactionProcessed.class);
    }

    @Override
    public Flux<TransactionProcessed> findWithoutInitiativesWithBatch(int batchSize){
        Query query = Query.query(Criteria.where(TransactionProcessed.Fields.initiatives).size(0)).cursorBatchSize(batchSize);
        return mongoTemplate.find(query, TransactionProcessed.class);
    }
}
