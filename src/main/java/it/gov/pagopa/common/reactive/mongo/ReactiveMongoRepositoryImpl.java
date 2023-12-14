package it.gov.pagopa.common.reactive.mongo;

import com.mongodb.client.result.DeleteResult;
import lombok.NonNull;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.io.Serializable;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class ReactiveMongoRepositoryImpl<E, I extends Serializable> extends SimpleReactiveMongoRepository<E, I> implements ReactiveMongoRepositoryExt<E, I> {

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

    @Override
    public Mono<DeleteResult> removeById(I id) {
        Assert.notNull(id, "The given id must not be null");

        return mongoOperations
                .remove(getIdQuery(id), entityInformation.getJavaType(), entityInformation.getCollectionName());
    }
    private Query getIdQuery(Object id) {
        return new Query(getIdCriteria(id));
    }

    private Criteria getIdCriteria(Object id) {
        return where(entityInformation.getIdAttribute()).is(id);
    }

}
