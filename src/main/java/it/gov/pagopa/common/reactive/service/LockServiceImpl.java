package it.gov.pagopa.common.reactive.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class LockServiceImpl implements LockService {

    private final int bucketSize;
    private final int maxThreads;
    private final int lockSecondsTimeout;
    private final Map<Integer, Semaphore> locks;
    private final Map<Integer, Scheduler> schedulers;

    public LockServiceImpl(
            @Value("${app.trx-lock.bucket-size}") int bucketSize,
            @Value("${app.trx-lock.max-threads}") int maxThreads,
            @Value("${app.trx-lock.timeout}") int lockSecondsTimeout
    ) {
        this.locks = IntStream.range(0, bucketSize)
                .mapToObj(i-> Pair.of(i, new Semaphore(1, true)))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

        this.bucketSize = bucketSize;
        this.maxThreads = maxThreads;
        this.lockSecondsTimeout = lockSecondsTimeout;
        this.schedulers = new ConcurrentHashMap<>(maxThreads);
    }

    @PreDestroy
    void releaseResources(){
        this.schedulers.values().forEach(Scheduler::dispose);
        this.schedulers.clear();
    }

    @Override
    public int getBuketSize() {
        return bucketSize;
    }

    @Override
    public Mono<Integer> acquireLock(int lockId) {
        return Mono.just(lockId)
                .publishOn(getScheduler(lockId))
                .doOnNext(this::blockCurrentThread);
    }

    private Scheduler getScheduler(int lockId) {
        int schedulerId = lockId % maxThreads;
        return schedulers.computeIfAbsent(schedulerId, x -> Schedulers.newBoundedElastic(1, 500, "lockedQueue-"+schedulerId));
    }

    private void blockCurrentThread(int lockId) {
        final Semaphore lock = locks.get(lockId);
        if(lock!=null){
            try {
                log.trace("Acquiring lock having id {}", lockId);
                if(!lock.tryAcquire(lockSecondsTimeout, TimeUnit.SECONDS)){
                    log.warn("lock timeout occurred at lockId {}", lockId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Something gone wrong while acquiring lock", e);
            }
        } else {
            throw new IllegalArgumentException("invalid lockId, its value should be between 0 and %d, provided value is %d".formatted(locks.size(), lockId));
        }
    }

    @Override
    public void releaseLock(int lockId) {
        final Semaphore lock = locks.get(lockId);
        if(lock!=null){
            if(log.isTraceEnabled()){
                log.trace("Releasing lock having id {}. Queue size {}", lockId, lock.getQueueLength());
            }
            lock.release();
        } else {
            throw new IllegalArgumentException("invalid lockId, its value should be between 0 and %d, provided value is %d".formatted(locks.size(), lockId));
        }
    }
}
