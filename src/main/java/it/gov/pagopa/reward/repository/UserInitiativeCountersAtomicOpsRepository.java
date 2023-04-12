package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionRequestDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import reactor.core.publisher.Mono;

public interface UserInitiativeCountersAtomicOpsRepository {
    Mono<UserInitiativeCounters> updateDate(SynchronousTransactionRequestDTO request, String initiativeId);

}
