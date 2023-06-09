package it.gov.pagopa.common.reactive.service;

import reactor.core.publisher.Mono;

public interface LockService {
    int getBuketSize();
    Mono<Integer> acquireLock(int lockId);
    void releaseLock(int lockId);
}
