package it.gov.pagopa.reward.connector.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import reactor.core.publisher.Mono;

public interface HpanInitiativesAtomicOpsRepository {
    Mono<UpdateResult> createIfNotExist(HpanInitiatives hpanInitiatives);
    Mono<UpdateResult> setInitiative(String hpan, OnboardedInitiative onboardedInitiative);
    Mono<UpdateResult> setStatus(String userId, String initiativeId, HpanInitiativeStatus status);
}
