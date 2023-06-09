package it.gov.pagopa.reward.connector.repository;

import reactor.core.publisher.Mono;

public interface TransactionRepository {
    Mono<Boolean> checkIfExists(String trxId);
}
