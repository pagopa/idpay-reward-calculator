package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.common.reactive.mongo.retry.MongoRequestRateTooLargeRetryer;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.utils.MongoRequestRateTooLargeUtilities;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface DroolsRuleRepository extends ReactiveMongoRepository<DroolsRule,String> {

  default <S extends DroolsRule> Mono<S> saveRetryable(S entity) {
    return MongoRequestRateTooLargeRetryer.withRetry(save(entity),
        MongoRequestRateTooLargeUtilities.maxRetry, MongoRequestRateTooLargeUtilities.maxMillisElapsed);
  }
}
