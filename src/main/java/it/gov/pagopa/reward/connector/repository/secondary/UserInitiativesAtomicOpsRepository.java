package it.gov.pagopa.reward.connector.repository.secondary;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.enums.OnboardingStatus;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.UserInitiatives;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserInitiativesAtomicOpsRepository {
    Mono<UpdateResult> createIfNotExist(UserInitiatives userInitiatives);
    Mono<UpdateResult> setInitiative(String userId, OnboardedInitiative onboardedInitiative);
    Mono<UserInitiatives> setUserInitiativeStatus(String userId, String initiativeId, OnboardingStatus status);
    Mono<Void> removeInitiativeForUser(String userId, String initiativeId);
    Flux<UserInitiatives> deleteWithoutInitiatives();
    Flux<UserInitiatives> findByInitiativesWithBatch(String initiativeId, int batchSize);
    Flux<UserInitiatives> findWithoutInitiativesWithBatch(int batchSize);
}
