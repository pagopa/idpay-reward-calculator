package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.model.HpanInitiatives;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface HpanInitiativesRepository extends ReactiveMongoRepository<HpanInitiatives,String>, HpanInitiativesAtomicOpsRepository {
    @Query(value="{'hpan' : ?0, 'onboardedInitiatives' : { $elemMatch : { 'initiativeId' : ?1}}}")
    Mono<HpanInitiatives> findByHpanAndInitiativeId(String hpan, String initiativeId);
}
