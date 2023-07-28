package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.model.HpanInitiatives;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface HpanInitiativesRepository extends ReactiveMongoRepository<HpanInitiatives,String>, HpanInitiativesAtomicOpsRepository {
}
