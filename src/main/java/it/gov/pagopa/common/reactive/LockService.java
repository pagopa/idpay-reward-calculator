package it.gov.pagopa.common.reactive;

import reactor.core.publisher.Mono;

public interface LockService {
    int getBuketSize();
    Mono<Integer> acquireLock(int lockId);
    void releaseLock(int lockId);
}
