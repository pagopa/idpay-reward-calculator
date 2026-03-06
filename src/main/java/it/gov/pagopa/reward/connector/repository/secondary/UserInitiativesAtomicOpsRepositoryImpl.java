package it.gov.pagopa.reward.connector.repository.secondary;

import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.enums.OnboardingStatus;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.UserInitiatives;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class UserInitiativesAtomicOpsRepositoryImpl implements UserInitiativesAtomicOpsRepository {

    public static final String FIELD_INITIATIVE_ID = "%s.%s".formatted(UserInitiatives.Fields.onboardedInitiatives, OnboardedInitiative.Fields.initiativeId);
    public static final String FIELD_INTERNAL_STATUS = "%s.$.%s".formatted(UserInitiatives.Fields.onboardedInitiatives, OnboardedInitiative.Fields.status);
    public static final String FIELD_INTERNAL_UPDATE_DATE = "%s.$.%s".formatted(UserInitiatives.Fields.onboardedInitiatives, OnboardedInitiative.Fields.updateDate);

    private final ReactiveMongoTemplate mongoTemplate;

    public UserInitiativesAtomicOpsRepositoryImpl(@Qualifier("secondaryReactiveMongoTemplate") ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<UpdateResult> createIfNotExist(UserInitiatives userInitiatives) {
        return mongoTemplate
                .upsert(
                        Query.query(Criteria.where("_id").is(userInitiatives.getUserId())),
                        new Update()
                                .setOnInsert("_id", userInitiatives.getUserId()),
                        UserInitiatives.class)
                .onErrorResume(e -> {
                    if (e instanceof DuplicateKeyException) {
                        return Mono.just(UpdateResult.acknowledged(1, 0L, null));
                    } else {
                        return Mono.error(e);
                    }
                });
    }

    @Override
    public Mono<UpdateResult> setInitiative(String userId, OnboardedInitiative onboardedInitiative) {
        if (onboardedInitiative.getActiveTimeIntervals().isEmpty()) {
            return mongoTemplate
                    .updateFirst(
                            Query.query(Criteria.where("_id").is(userId)),
                            new Update()
                                    .pull(UserInitiatives.Fields.onboardedInitiatives,
                                            Query.query(Criteria.where(OnboardedInitiative.Fields.initiativeId).is(onboardedInitiative.getInitiativeId()))),
                            UserInitiatives.class);
        } else {
            return mongoTemplate
                    .updateFirst(
                            Query.query(Criteria.where("_id").is(userId)
                                    .and(FIELD_INITIATIVE_ID).ne(onboardedInitiative.getInitiativeId())),
                            new Update().push(UserInitiatives.Fields.onboardedInitiatives, onboardedInitiative),
                            UserInitiatives.class)
                    .flatMap(ur -> {
                        if (ur.getModifiedCount() > 0) {
                            return Mono.just(ur);
                        } else {
                            return mongoTemplate
                                    .updateFirst(
                                            Query.query(
                                                    Criteria.where("_id").is(userId)
                                                            .and(FIELD_INITIATIVE_ID).is(onboardedInitiative.getInitiativeId())),
                                            new Update()
                                                    .set("%s.$".formatted(UserInitiatives.Fields.onboardedInitiatives), onboardedInitiative),
                                            UserInitiatives.class);
                        }
                    });
        }
    }

    @Override
    public Mono<UserInitiatives> setUserInitiativeStatus(String userId, String initiativeId, OnboardingStatus status) {
        Query query = Query.query(Criteria.where("_id").is(userId)
                .and(FIELD_INITIATIVE_ID).is(initiativeId));
        return mongoTemplate.findAndModify(
                query,
                new Update()
                        .set(FIELD_INTERNAL_STATUS, status)
                        .currentDate(FIELD_INTERNAL_UPDATE_DATE),
                UserInitiatives.class);
    }

    @Override
    public Mono<Void> removeInitiativeForUser(String userId, String initiativeId) {
        return mongoTemplate
                .updateFirst(
                        Query.query(Criteria.where("_id").is(userId)),
                        new Update().pull(UserInitiatives.Fields.onboardedInitiatives,
                                new BasicDBObject(OnboardedInitiative.Fields.initiativeId, initiativeId)),
                        UserInitiatives.class)
                .then();
    }

    @Override
    public Flux<UserInitiatives> deleteWithoutInitiatives() {
        return mongoTemplate
                .findAllAndRemove(
                        Query.query(
                                Criteria.where(UserInitiatives.Fields.onboardedInitiatives)
                                        .size(0)),
                        UserInitiatives.class);
    }

    @Override
    public Flux<UserInitiatives> findByInitiativesWithBatch(String initiativeId, int batchSize) {
        Query query = Query.query(Criteria.where(UserInitiatives.Fields.onboardedInitiatives)
                .elemMatch(Criteria.where(OnboardedInitiative.Fields.initiativeId).is(initiativeId)))
                .cursorBatchSize(batchSize);
        return mongoTemplate.find(query, UserInitiatives.class);
    }

    @Override
    public Flux<UserInitiatives> findWithoutInitiativesWithBatch(int batchSize) {
        Query query = Query.query(Criteria.where(UserInitiatives.Fields.onboardedInitiatives).size(0))
                .cursorBatchSize(batchSize);
        return mongoTemplate.find(query, UserInitiatives.class);
    }
}
