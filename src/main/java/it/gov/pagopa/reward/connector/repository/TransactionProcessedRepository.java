package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.common.reactive.mongo.retry.MongoRequestRateTooLargeRetryer;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.utils.MongoRequestRateTooLargeUtilities;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionProcessedRepository  extends ReactiveMongoRepository<BaseTransactionProcessed,String> {

    default Flux<BaseTransactionProcessed> findByAcquirerIdAndCorrelationIdRetryable(String acquirerId,
        String correlationId) {
        return MongoRequestRateTooLargeRetryer.withRetry(findByAcquirerIdAndCorrelationId(acquirerId, correlationId), MongoRequestRateTooLargeUtilities.maxRetry, MongoRequestRateTooLargeUtilities.maxMillisElapsed);
    }

    Flux<BaseTransactionProcessed> findByAcquirerIdAndCorrelationId(String acquirerId, String correlationId);

    default <S extends BaseTransactionProcessed> Mono<S> saveRetryable(S entity) {
        return MongoRequestRateTooLargeRetryer.withRetry(save(entity), MongoRequestRateTooLargeUtilities.maxRetry, MongoRequestRateTooLargeUtilities.maxMillisElapsed);
    }

    default Mono<BaseTransactionProcessed> findByIdRetryable(String s) {
        return MongoRequestRateTooLargeRetryer.withRetry(findById(s), MongoRequestRateTooLargeUtilities.maxRetry, MongoRequestRateTooLargeUtilities.maxMillisElapsed);
    }
}
