package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface UserInitiativeCountersRepository extends ReactiveMongoRepository<UserInitiativeCounters,String>, UserInitiativeCountersAtomicOpsRepository {
    Flux<UserInitiativeCounters> findByUserId(String userId);
    Flux<UserInitiativeCounters> findByUserIdAndInitiativeIdIn(String userId, Collection<String> initiativeIds);
}
