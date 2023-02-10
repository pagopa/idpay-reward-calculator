package it.gov.pagopa.reward.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class PerformanceLoggerTest {

//region mono ops
    @Test
    void testMonoLogTimingOnNext(){
        boolean[] result=new boolean[]{false};
        Mono<Boolean> mono = PerformanceLogger.logTimingOnNext("PROVA", Mono.defer(() -> Mono.just(result[0] = true)), null);
        Assertions.assertFalse(result[0]);
        mono.block();
        Assertions.assertTrue(result[0]);
    }

    @Test
    void testMonoLogTimingFinally(){
        boolean[] result=new boolean[]{false};
        Mono<Boolean> mono = PerformanceLogger.logTimingFinally("PROVA", Mono.defer(() -> Mono.just(result[0] = true)), "END");
        Assertions.assertFalse(result[0]);
        mono.block();
        Assertions.assertTrue(result[0]);
    }
//endregion

    //region flux ops
    @Test
    void testFlux(){
        boolean[] result=new boolean[]{false};
        Flux<Boolean> flux = PerformanceLogger.logTimingOnNext("PROVA", Flux.defer(() -> Flux.just(result[0] = true)), null);
        Assertions.assertFalse(result[0]);
        flux.collectList().block();
        Assertions.assertTrue(result[0]);
    }

    @Test
    void testFluxLogTimingFinally(){
        boolean[] result=new boolean[]{false};
        Flux<Boolean> flux = PerformanceLogger.logTimingFinally("PROVA", Flux.defer(() -> Flux.just(result[0] = true)), "END");
        Assertions.assertFalse(result[0]);
        flux.collectList().block();
        Assertions.assertTrue(result[0]);
    }
//endregion
}
