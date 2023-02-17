package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface TransactionProcessedRepository  extends ReactiveMongoRepository<BaseTransactionProcessed,String> {
    Flux<BaseTransactionProcessed> findByAcquirerIdAndCorrelationId(String acquirerId, String correlationId);
}
