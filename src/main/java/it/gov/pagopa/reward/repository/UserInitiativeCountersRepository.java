package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface UserInitiativeCountersRepository extends ReactiveMongoRepository<UserInitiativeCounters,String> {
}
