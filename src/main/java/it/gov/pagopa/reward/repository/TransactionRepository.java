package it.gov.pagopa.reward.repository;

import reactor.core.publisher.Mono;

public interface TransactionRepository {
    Mono<Boolean> checkIfExists(String trxId);
}
