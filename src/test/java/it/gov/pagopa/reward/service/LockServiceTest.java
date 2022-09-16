package it.gov.pagopa.reward.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class LockServiceTest {

    public static final int WAIT_TIMEOUT = 3;
    public static final int LOCK_SIZE = 10;

    private final LockService lockService = new LockServiceImpl(LOCK_SIZE, WAIT_TIMEOUT);

    @BeforeAll
    static void traceLogs(){
        ((Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.service.LockServiceImpl")).setLevel(Level.TRACE);
    }

    @AfterAll
    static void resetLogs(){
        ((Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.service.LockServiceImpl")).setLevel(Level.INFO);
    }

    @Test
    void test() {
        Assertions.assertEquals(LOCK_SIZE, lockService.getBuketSize());

        // testing lock
        Assertions.assertThrows(IllegalArgumentException.class, () -> lockService.acquireLock(-1));

        lockService.acquireLock(0);
        ExecutorService executor = null;
        try {
            executor = Executors.newFixedThreadPool(1);
            final Future<?> lockedThread = executor.submit(() -> lockService.acquireLock(0));
            Awaitility.await().atLeast(WAIT_TIMEOUT-1, TimeUnit.SECONDS).until(() -> {
                lockedThread.get();
                return true;
            });

            // testing release
            lockService.releaseLock(0);

            Assertions.assertThrows(IllegalArgumentException.class, () -> lockService.releaseLock(LOCK_SIZE));

            final Future<?> unlockedThread = executor.submit(() -> lockService.acquireLock(0));
            Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> {
                unlockedThread.get();
                return true;
            });

        } finally {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
            }
        }
    }

}
