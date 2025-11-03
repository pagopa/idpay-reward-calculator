package it.gov.pagopa.reward.connector.repository.primary;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.exception.custom.InvalidCounterVersionException;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class UserInitiativeCountersAtomicOpsRepositoryImpl implements UserInitiativeCountersAtomicOpsRepository {
    private static final String PENDING_TRX_ID_FIELD = String.format("%s.%s", UserInitiativeCounters.Fields.pendingTrx, TransactionDTO.Fields.id);
    private final ReactiveMongoTemplate mongoTemplate;

    public UserInitiativeCountersAtomicOpsRepositoryImpl(@Qualifier("reactiveMongoTemplate")
            ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<UpdateResult> createIfNotExists(String entityId, InitiativeGeneralDTO.BeneficiaryTypeEnum entityType, String initiativeId) {
        String counterId = UserInitiativeCounters.buildId(entityId, initiativeId);
        return mongoTemplate
                    .upsert(
                            Query.query(Criteria
                                    .where(UserInitiativeCounters.Fields.id).is(counterId)),
                            new Update()
                                    .set(UserInitiativeCounters.Fields.entityId, entityId)
                                    .setOnInsert(UserInitiativeCounters.Fields.entityType, entityType)
                                    .setOnInsert(UserInitiativeCounters.Fields.initiativeId, initiativeId)
                                    .set(UserInitiativeCounters.Fields.version, 0L)
                                    .set(UserInitiativeCounters.Fields.exhaustedBudget, false)
                                    .set(UserInitiativeCounters.Fields.pendingTrx, null)
                                    .set(UserInitiativeCounters.Fields.lastTrx, new ArrayList<>())
                                    .set(UserInitiativeCounters.Fields.dailyCounters, new HashMap<>())
                                    .set(UserInitiativeCounters.Fields.weeklyCounters, new HashMap<>())
                                    .set(UserInitiativeCounters.Fields.monthlyCounters, new HashMap<>())
                                    .set(UserInitiativeCounters.Fields.yearlyCounters, new HashMap<>())
                                    .set(Counters.Fields.trxNumber, 0L)
                                    .set(Counters.Fields.totalAmountCents, 0L)
                                    .set(Counters.Fields.totalRewardCents, 0L),
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
                        FindAndModifyOptions.options().returnNew(true),
                        UserInitiativeCounters.class
                );
    }

    @Override
    public Mono<UserInitiativeCounters> findByPendingTrx(String trxId){
        Query query = Query.query(Criteria.where(PENDING_TRX_ID_FIELD).is(trxId));
        return mongoTemplate.findOne(query, UserInitiativeCounters.class);
    }

    @Override
    public Mono<UserInitiativeCounters> saveIfVersionNotChanged(UserInitiativeCounters counters) {
        return mongoTemplate
                .findAndReplace(
                        Query.query(Criteria.where(UserInitiativeCounters.Fields.id).is(counters.getId())
                                .and(UserInitiativeCounters.Fields.version).is(counters.getVersion()-1L)),
                        counters,
                        FindAndReplaceOptions.options().returnNew(),
                        UserInitiativeCounters.class,
                        UserInitiativeCounters.class)
                .switchIfEmpty(Mono.error(new InvalidCounterVersionException()));
    }

    @Override
    public Mono<UserInitiativeCounters> updateEntityIdByInitiativeIdAndEntityId(String initiativeId, String entityId, String newEntityId) {

        Query query = Query.query(Criteria.where(UserInitiativeCounters.Fields.entityId).is(entityId)
                .and(UserInitiativeCounters.Fields.initiativeId).is(initiativeId));
        return mongoTemplate.findAndModify(
                query, new Update()
                        .set(UserInitiativeCounters.Fields.entityId, newEntityId),
                UserInitiativeCounters.class);

    }
}
