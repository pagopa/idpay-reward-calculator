package it.gov.pagopa.reward.connector.repository.secondary;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface HpanInitiativesAtomicOpsRepository {
    Mono<UpdateResult> createIfNotExist(HpanInitiatives hpanInitiatives);
    Mono<UpdateResult> setInitiative(String hpan, OnboardedInitiative onboardedInitiative);
    Mono<UpdateResult> setUserInitiativeStatus(String userId, String initiativeId, HpanInitiativeStatus status);
    Mono<Void> removeInitiativeOnHpan(String hpan, String initiativeId);
    Flux<HpanInitiatives> deleteHpanWithoutInitiative();
    Flux<HpanInitiatives> findByInitiativesWithBatch(String initiativeId, int batchSize);
    Flux<HpanInitiatives> findWithoutInitiativesWithBatch(int batchSize);
}
