package it.gov.pagopa.reward.connector.repository.primary;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TransactionRepositoryImpl implements TransactionRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public TransactionRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Boolean> checkIfExists(String trxId) {
        return mongoTemplate.exists(Query.query(Criteria.where("_id").is(trxId)), "transaction");
    }
}
