package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import reactor.core.publisher.Mono;

public interface UserInitiativeCountersAtomicOpsRepository {
    Mono<UserInitiativeCounters> findByIdThrottled(String id);

}
