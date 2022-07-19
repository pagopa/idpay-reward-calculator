package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.model.CitizenHpan;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CitizenHpanRepository extends ReactiveMongoRepository<CitizenHpan,String> {
}
