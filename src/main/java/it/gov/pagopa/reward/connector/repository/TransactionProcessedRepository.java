package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.common.reactive.mongo.retry.MongoRequestRateTooLargeRetryer;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.common.utils.MongoRequestRateTooLargeConfig;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionProcessedRepository  extends ReactiveMongoRepository<BaseTransactionProcessed,String> {

    default Flux<BaseTransactionProcessed> findByAcquirerIdAndCorrelationIdRetryable(String acquirerId,
        String correlationId) {
        return MongoRequestRateTooLargeRetryer.withRetry(findByAcquirerIdAndCorrelationId(acquirerId, correlationId), MongoRequestRateTooLargeConfig.maxRetry, MongoRequestRateTooLargeConfig.maxMillisElapsed);
    }

    Flux<BaseTransactionProcessed> findByAcquirerIdAndCorrelationId(String acquirerId, String correlationId);

    default <S extends BaseTransactionProcessed> Mono<S> saveRetryable(S entity) {
        return MongoRequestRateTooLargeRetryer.withRetry(save(entity), MongoRequestRateTooLargeConfig.maxRetry, MongoRequestRateTooLargeConfig.maxMillisElapsed);
    }

    default Mono<BaseTransactionProcessed> findByIdRetryable(String s) {
        return MongoRequestRateTooLargeRetryer.withRetry(findById(s), MongoRequestRateTooLargeConfig.maxRetry, MongoRequestRateTooLargeConfig.maxMillisElapsed);
    }
}
