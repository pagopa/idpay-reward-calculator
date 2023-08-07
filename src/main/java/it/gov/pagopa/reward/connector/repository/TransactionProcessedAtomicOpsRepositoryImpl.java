package it.gov.pagopa.reward.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.model.TransactionProcessed;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TransactionProcessedAtomicOpsRepositoryImpl implements TransactionProcessedAtomicOpsRepository {
    private final ReactiveMongoTemplate mongoTemplate;

    public TransactionProcessedAtomicOpsRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Flux<TransactionProcessed> deleteByInitiativeId(String initiativeId) {
        return mongoTemplate
                .findAllAndRemove(
                        Query.query(Criteria.where(TransactionProcessed.Fields.initiatives).is(initiativeId)),
                        TransactionProcessed.class
                );
    }

    @Override
    public Mono<UpdateResult> findAndRemoveInitiativeOnTransaction(String initiativeId) {
        return mongoTemplate
                .updateMulti(
                        Query.query(Criteria.where(TransactionProcessed.Fields.initiatives).is(initiativeId)),
                        new Update()
                                .pull(TransactionProcessed.Fields.initiatives, initiativeId),
                        TransactionProcessed.class
                );
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
}
