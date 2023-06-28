package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.model.HpanInitiatives;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface HpanInitiativesRepository extends ReactiveMongoRepository<HpanInitiatives,String>, HpanInitiativesAtomicOpsRepository {
    @Query(value = "{userId : ?0, 'onboardedInitiatives.initiativeId' : ?1}")
    Flux<HpanInitiatives> retrieveHpanByUserIdAndInitiativeId(String userId, String initiativeId);
}
