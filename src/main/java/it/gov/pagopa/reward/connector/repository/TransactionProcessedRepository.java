package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface TransactionProcessedRepository  extends ReactiveMongoRepository<BaseTransactionProcessed,String>, TransactionProcessedAtomicOpsRepository {
    Flux<BaseTransactionProcessed> findByAcquirerIdAndCorrelationId(String acquirerId, String correlationId);
}
