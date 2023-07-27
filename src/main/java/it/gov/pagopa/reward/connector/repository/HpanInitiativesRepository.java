package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.common.reactive.mongo.retry.MongoRequestRateTooLargeRetryer;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.utils.MongoRequestRateTooLargeUtilities;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface HpanInitiativesRepository extends ReactiveMongoRepository<HpanInitiatives,String>, HpanInitiativesAtomicOpsRepository {

  default Mono<HpanInitiatives> findByIdRetryable(String s) {
    return MongoRequestRateTooLargeRetryer.withRetry(findById(s), MongoRequestRateTooLargeUtilities.maxRetry, MongoRequestRateTooLargeUtilities.maxMillisElapsed);
  }
}
