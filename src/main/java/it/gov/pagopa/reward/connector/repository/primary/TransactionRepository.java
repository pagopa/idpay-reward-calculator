package it.gov.pagopa.reward.connector.repository.primary;

import reactor.core.publisher.Mono;

public interface TransactionRepository {
    Mono<Boolean> checkIfExists(String trxId);
}
