package it.gov.pagopa.common.reactive.mongo;

import lombok.NonNull;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public class ReactiveMongoRepositoryImpl<E, I extends Serializable> extends SimpleReactiveMongoRepository<E, I> {

    private final ReactiveMongoOperations mongoOperations;
    private final MongoEntityInformation<E, I> entityInformation;

    public ReactiveMongoRepositoryImpl(MongoEntityInformation<E, I> entityInformation, ReactiveMongoOperations mongoOperations) {
        super(entityInformation, mongoOperations);

        this.mongoOperations = mongoOperations;
        this.entityInformation = entityInformation;
    }

    @Override
    public @NonNull Mono<E> findById(@NonNull I id) {
        return mongoOperations.find(
                new Query(Criteria.where("_id").is(id)).cursorBatchSize(0),
                entityInformation.getJavaType(), entityInformation.getCollectionName()).singleOrEmpty();
    }

}
