package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import reactor.core.publisher.Mono;

public interface UserInitiativeCountersAtomicOpsRepository {
    Mono<UserInitiativeCounters> findByThrottled(String id);

}
