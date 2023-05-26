package it.gov.pagopa.common.reactive.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

class PerformanceLoggerTest {

//region mono ops
    @Test
    void testMonoLogTimingOnNext(){
        testMonoLogTimingOnNext(null);
        testMonoLogTimingOnNext(Object::toString);
    }

    private static void testMonoLogTimingOnNext(Function<Boolean, String> data2LogPayload) {
        boolean[] result=new boolean[]{false};
        Mono<Boolean> mono = PerformanceLogger.logTimingOnNext("PROVA", Mono.defer(() -> Mono.just(result[0] = true)), data2LogPayload);
        Assertions.assertFalse(result[0]);
        mono.block();
        Assertions.assertTrue(result[0]);
    }

    @Test
    void testMonoLogTimingFinally(){
        testMonoLogTimingFinally(null);
        testMonoLogTimingFinally("END");
    }

    private static void testMonoLogTimingFinally(String logPayload) {
        boolean[] result=new boolean[]{false};
        Mono<Boolean> mono = PerformanceLogger.logTimingFinally("PROVA", Mono.defer(() -> Mono.just(result[0] = true)), logPayload);
        Assertions.assertFalse(result[0]);
        mono.block();
        Assertions.assertTrue(result[0]);
    }
//endregion

    //region flux ops
    @Test
    void testFluxLogTimingOnNext(){
        testFluxLogTimingOnNext(null);
        testFluxLogTimingOnNext(Object::toString);
    }

    private static void testFluxLogTimingOnNext(Function<Boolean, String> data2LogPayload) {
        boolean[] result=new boolean[]{false};
        Flux<Boolean> flux = PerformanceLogger.logTimingOnNext("PROVA", Flux.defer(() -> Flux.just(result[0] = true)), data2LogPayload);
        Assertions.assertFalse(result[0]);
        flux.collectList().block();
        Assertions.assertTrue(result[0]);
    }

    @Test
    void testFluxLogTimingFinally(){
        testFluxLogTimingFinally(null);
        testFluxLogTimingFinally("END");
    }

    private static void testFluxLogTimingFinally(String logPayload) {
        boolean[] result=new boolean[]{false};
        Flux<Boolean> flux = PerformanceLogger.logTimingFinally("PROVA", Flux.defer(() -> Flux.just(result[0] = true)), logPayload);
        Assertions.assertFalse(result[0]);
        flux.collectList().block();
        Assertions.assertTrue(result[0]);
    }
//endregion
}
