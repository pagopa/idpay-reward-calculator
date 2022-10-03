package it.gov.pagopa.reward.repository;

import com.mongodb.client.result.UpdateResult;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import reactor.core.publisher.Mono;

public interface HpanInitiativesAtomicOpsRepository {
    Mono<UpdateResult> createIfNotExist(HpanInitiatives hpanInitiatives);
    Mono<UpdateResult> setInitiative(String hpan, OnboardedInitiative onboardedInitiative);
}
