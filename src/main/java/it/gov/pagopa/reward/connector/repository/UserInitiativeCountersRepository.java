package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface UserInitiativeCountersRepository extends ReactiveMongoRepository<UserInitiativeCounters,String>, UserInitiativeCountersAtomicOpsRepository {
    Flux<UserInitiativeCounters> findByEntityId(String entityId);
    Flux<UserInitiativeCounters> findByEntityIdAndInitiativeIdIn(String entityId, Collection<String> initiativeIds);
    Flux<UserInitiativeCounters> deleteByInitiativeId(String initiativeId);
}
