package it.gov.pagopa.reward.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

public class HpanInitiativesAtomicOpsRepositoryImpl implements HpanInitiativesAtomicOpsRepository {
    public static final String FIELD_INITIATIVE_ID = "%s.%s".formatted(HpanInitiatives.Fields.onboardedInitiatives, OnboardedInitiative.Fields.initiativeId);
    private final ReactiveMongoTemplate mongoTemplate;

    public HpanInitiativesAtomicOpsRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<UpdateResult> createIfNotExist(HpanInitiatives hpanInitiatives) {
        return mongoTemplate
                .upsert(
                        Query.query(Criteria
                                .where(HpanInitiatives.Fields.hpan).is(hpanInitiatives.getHpan())),
                        new Update()
                                .setOnInsert(HpanInitiatives.Fields.userId, hpanInitiatives.getUserId())
                                .setOnInsert(HpanInitiatives.Fields.hpan, hpanInitiatives.getHpan())
                                .setOnInsert(HpanInitiatives.Fields.maskedPan, hpanInitiatives.getMaskedPan())
                                .setOnInsert(HpanInitiatives.Fields.brandLogo, hpanInitiatives.getBrandLogo()),
                        HpanInitiatives.class).onErrorResume(e -> {
                    if (e instanceof DuplicateKeyException) {
                        return Mono.just(UpdateResult.acknowledged(1, 0L, null));
                    } else {
                        return Mono.error(e);
                    }
                });
    }

    @Override
    public Mono<UpdateResult> setInitiative(String hpan, OnboardedInitiative onboardedInitiative) {
        if (onboardedInitiative.getActiveTimeIntervals().isEmpty()) {
            return mongoTemplate
                    .updateFirst(
                            Query.query(Criteria.where(HpanInitiatives.Fields.hpan).is(hpan)),
                            new Update()
                                    .pull(HpanInitiatives.Fields.onboardedInitiatives,
                                            Query.query(Criteria.where(OnboardedInitiative.Fields.initiativeId).is(onboardedInitiative.getInitiativeId()))),
                            HpanInitiatives.class);
        } else {
            return mongoTemplate
                    .updateFirst(
                            Query.query(Criteria.where(HpanInitiatives.Fields.hpan).is(hpan)
                                    .and(FIELD_INITIATIVE_ID).ne(onboardedInitiative.getInitiativeId())),
                            new Update().push(HpanInitiatives.Fields.onboardedInitiatives, onboardedInitiative),
                            HpanInitiatives.class)
                    .flatMap(ur -> {
                        if (ur.getModifiedCount() > 0) {
                            return Mono.just(ur);
                        } else {
                            return mongoTemplate
                                    .updateFirst(
                                            Query.query(
                                                    Criteria.where(HpanInitiatives.Fields.hpan).is(hpan)
                                                            .and(FIELD_INITIATIVE_ID).is(onboardedInitiative.getInitiativeId())),
                                            new Update()
                                                    .set("%s.$".formatted(HpanInitiatives.Fields.onboardedInitiatives), onboardedInitiative),
                                            HpanInitiatives.class);
                        }
                    });
        }
    }
}
