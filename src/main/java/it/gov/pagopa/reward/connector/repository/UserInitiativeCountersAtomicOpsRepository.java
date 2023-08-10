package it.gov.pagopa.reward.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import reactor.core.publisher.Mono;

public interface UserInitiativeCountersAtomicOpsRepository {
    Mono<UserInitiativeCounters> findByIdThrottled(String id, String updatingTrxId);
    Mono<UserInitiativeCounters> setUpdatingTrx(String id, String updatingTrxId);

    Mono<UpdateResult> createIfNotExists(String entityId, InitiativeGeneralDTO.BeneficiaryTypeEnum entityType, String initiativeId);
}
