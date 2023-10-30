package it.gov.pagopa.common.mongo.singleinstance;

import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.spring.autoconfigure.MongodWrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleInstanceMongodWrapper extends MongodWrapper {

    private final Runnable unprotectedStart;
    private final Runnable unprotectedStop;

    private final AtomicInteger counter = new AtomicInteger(0);
    public static Net singleMongodNet;

    public SingleInstanceMongodWrapper(MongodWrapper mongodWrapper) {
        super(null);

        unprotectedStart = unprotectMongodWrapperMethod("start", mongodWrapper);
        unprotectedStop = unprotectMongodWrapperMethod("stop", mongodWrapper);
    }

    private Runnable unprotectMongodWrapperMethod(String methodName, MongodWrapper mongodWrapper) {
        try {
            Method method = MongodWrapper.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return () -> {
                try {
                    method.invoke(mongodWrapper);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Cannot invoke protected %s mongodWrapper method".formatted(methodName), e);
                }
            };
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot unprotect mongodWrapper methods", e);
        }
    }

    @SuppressWarnings("unused") // called by Spring
    private void start() {
        synchronized (counter) {
            if (counter.getAndIncrement() == 0) {
                unprotectedStart.run();
            }
        }
    }

    @SuppressWarnings("unused") // called by Spring
    private void stop() {
        if (counter.decrementAndGet() == 0) {
            unprotectedStop.run();
        }
    }
}
