package it.gov.pagopa.reward.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.common.mongo.utils.MongoConstants;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.exception.custom.TooManyRequestsException;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionMessage;
public class UserInitiativeCountersAtomicOpsRepositoryImpl implements UserInitiativeCountersAtomicOpsRepository {
    private static final String PENDING_TRX_ID_FIELD = String.format("%s.%s", UserInitiativeCounters.Fields.pendingTrx, TransactionDTO.Fields.id);
    private final int throttlingSeconds;
    private final ReactiveMongoTemplate mongoTemplate;

    public UserInitiativeCountersAtomicOpsRepositoryImpl(
            @Value("${app.synchronousTransactions.throttlingSeconds:1}") int throttlingSeconds,

            ReactiveMongoTemplate mongoTemplate) {
        this.throttlingSeconds = throttlingSeconds;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<UserInitiativeCounters> findByIdThrottled(String id, String updatingTrxId) {
        return mongoTemplate
                .findAndModify(
                        Query.query(criteriaById(id))
                                .addCriteria(
                                        Criteria.expr(
                                                ComparisonOperators.Lt.valueOf(UserInitiativeCounters.Fields.updateDate)
                                                        .lessThan(ArithmeticOperators.Subtract.valueOf(MongoConstants.AGGREGATION_EXPRESSION_VARIABLE_NOW).subtract(1000 * throttlingSeconds)))),
                        new Update()
                                .currentDate(UserInitiativeCounters.Fields.updateDate)
                                .push(UserInitiativeCounters.Fields.updatingTrxId).slice(1).each(updatingTrxId),
                        FindAndModifyOptions.options().returnNew(true),
                        UserInitiativeCounters.class
                )
                .switchIfEmpty(mongoTemplate.exists(Query.query(criteriaById(id)), UserInitiativeCounters.class)
                        .mapNotNull(counterExist -> {
                            if (Boolean.TRUE.equals(counterExist)) {
                                throw new TooManyRequestsException(ExceptionMessage.TOO_MANY_REQUESTS_MSG);
                            } else {
                                return null;
                            }
                        }));
    }

    private Criteria criteriaById(String id) {
        return Criteria
                .where(UserInitiativeCounters.Fields.id).is(id);
    }

    @Override
    public Mono<UserInitiativeCounters> setUpdatingTrx(String id, String updatingTrxId) {
        List<String> nextUpdatingTrx=null;
        if(updatingTrxId!=null){
            nextUpdatingTrx=List.of(updatingTrxId);
        }
        return mongoTemplate
                .findAndModify(
                        Query.query(criteriaById(id)),
                        new Update()
                                .currentDate(UserInitiativeCounters.Fields.updateDate)
                                .set(UserInitiativeCounters.Fields.updatingTrxId, nextUpdatingTrx),
                        FindAndModifyOptions.options().returnNew(true),
                        UserInitiativeCounters.class
                );
    }

    @Override
    public Mono<UpdateResult> createIfNotExists(String entityId, InitiativeGeneralDTO.BeneficiaryTypeEnum entityType, String initiativeId) {
        String counterId = UserInitiativeCounters.buildId(entityId, initiativeId);
        return mongoTemplate
                    .upsert(
                            Query.query(Criteria
                                    .where(UserInitiativeCounters.Fields.id).is(counterId)),
                            new Update()
                                    .setOnInsert(UserInitiativeCounters.Fields.entityId, entityId)
                                    .setOnInsert(UserInitiativeCounters.Fields.entityType, entityType)
                                    .setOnInsert(UserInitiativeCounters.Fields.initiativeId, initiativeId)
                                    .setOnInsert(UserInitiativeCounters.Fields.version, 0L)
                                    .setOnInsert(UserInitiativeCounters.Fields.exhaustedBudget, false)
                                    .setOnInsert(UserInitiativeCounters.Fields.updateDate, LocalDateTime.now()),
                            UserInitiativeCounters.class).onErrorResume(e -> {
                        if (e instanceof DuplicateKeyException) {
                            return Mono.just(UpdateResult.acknowledged(1, 0L, null));
                        } else {
                            return Mono.error(e);
                        }
                    });
        }

    @Override
    public Flux<UserInitiativeCounters> findByInitiativesWithBatch(String initiativeId, int batchSize){
        Query query = Query.query(Criteria.where(UserInitiativeCounters.Fields.initiativeId).is(initiativeId)).cursorBatchSize(batchSize);
        return mongoTemplate.find(query, UserInitiativeCounters.class);
    }

    @Override
    public Mono<UserInitiativeCounters> unlockPendingTrx(String trxId) {
        return mongoTemplate
                .findAndModify(
                        Query.query(Criteria.where(PENDING_TRX_ID_FIELD).is(trxId)),
                        new Update()
                                .currentDate(UserInitiativeCounters.Fields.updateDate)
                                .set(UserInitiativeCounters.Fields.pendingTrx, null),
                        UserInitiativeCounters.class
                );
    }
}
