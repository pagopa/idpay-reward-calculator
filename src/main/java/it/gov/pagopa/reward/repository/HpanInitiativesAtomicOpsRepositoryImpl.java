package it.gov.pagopa.reward.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

public class HpanInitiativesAtomicOpsRepositoryImpl implements HpanInitiativesAtomicOpsRepository{
    public static final String FIELD_INITIATIVE_ID = "%s.%s".formatted(HpanInitiatives.Fields.onboardedInitiatives, OnboardedInitiative.Fields.initiativeId);
    private final ReactiveMongoTemplate mongoTemplate;

    public HpanInitiativesAtomicOpsRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<HpanInitiatives> createIfNotExist(HpanInitiatives hpanInitiatives) {
        return mongoTemplate.save(hpanInitiatives); //TODO if works try using directly the repository save operation
    }

    @Override
    public Mono<UpdateResult> setInitiative(String hpan, OnboardedInitiative onboardedInitiative) {
        return mongoTemplate.updateFirst(
                Query.query(Criteria.where(HpanInitiatives.Fields.hpan).is(hpan)
                .and(FIELD_INITIATIVE_ID).is(onboardedInitiative.getInitiativeId())),
                new Update().set("%s.$".formatted(HpanInitiatives.Fields.onboardedInitiatives),onboardedInitiative),
                HpanInitiatives.class
        );
    }
}
