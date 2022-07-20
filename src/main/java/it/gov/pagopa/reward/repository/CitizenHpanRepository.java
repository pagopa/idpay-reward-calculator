package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.model.HpanInitiatives;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CitizenHpanRepository extends ReactiveMongoRepository<HpanInitiatives,String> {
}
