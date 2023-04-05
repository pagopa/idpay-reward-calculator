package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.model.HpanInitiatives;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface HpanInitiativesRepository extends ReactiveMongoRepository<HpanInitiatives,String>, HpanInitiativesAtomicOpsRepository {
    Mono<HpanInitiatives> findByHpanAndOnboardedInitiativesInitiativeIdIn(String hpan, Collection<String> initiativeIds);
}
