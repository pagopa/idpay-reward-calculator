package it.gov.pagopa.reward.connector.repository.secondary;

import it.gov.pagopa.reward.model.OnboardingFamilies;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface OnboardingFamiliesRepository extends ReactiveMongoRepository<OnboardingFamilies, String> {
    Flux<OnboardingFamilies> findByMemberIdsInAndInitiativeId(String memberId, String initiativeId);
}
