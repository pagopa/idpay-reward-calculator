package it.gov.pagopa.reward.repository;

import it.gov.pagopa.reward.model.TransactionProcessed;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface TransactionProcessedRepository  extends ReactiveMongoRepository<TransactionProcessed,String> {
}
