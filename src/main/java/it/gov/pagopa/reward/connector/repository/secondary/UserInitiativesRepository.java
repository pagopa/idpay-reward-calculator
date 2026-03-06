package it.gov.pagopa.reward.connector.repository.secondary;

import it.gov.pagopa.reward.model.UserInitiatives;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface UserInitiativesRepository extends ReactiveMongoRepository<UserInitiatives, String>, UserInitiativesAtomicOpsRepository {
}
