package it.gov.pagopa.common.reactive.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
public class PerformanceLogger {

    private PerformanceLogger(){}

//region Mono ops
    public static <T> Mono<T> logTimingOnNext(String flowName, Mono<T> publisher, Function<T, String> data2LogPayload){
        return logTimingOnNext(flowName, System.currentTimeMillis(), publisher, data2LogPayload);
    }
    public static <T> Mono<T> logTimingOnNext(String flowName, long startTime, Mono<T> publisher, Function<T, String> data2LogPayload){
        return publisher
                .doOnNext(x -> logTiming(flowName, startTime, data2LogPayload!=null? data2LogPayload.apply(x) : ""));
    }

    public static <T> Mono<T> logTimingFinally(String flowName, Mono<T> publisher, String logPayload){
        return logTimingFinally(flowName, System.currentTimeMillis(), publisher, logPayload);
    }
    public static <T> Mono<T> logTimingFinally(String flowName, long startTime, Mono<T> publisher, String logPayload){
        return publisher
                .doFinally(x -> logTiming(flowName, startTime, ObjectUtils.firstNonNull(logPayload, "")));
    }
//endregion

//region Flux ops
    public static <T> Flux<T> logTimingOnNext(String flowName, Flux<T> publisher, Function<T, String> data2LogPayload){
        return logTimingOnNext(flowName, System.currentTimeMillis(), publisher, data2LogPayload);
    }
    public static <T> Flux<T> logTimingOnNext(String flowName, long startTime, Flux<T> publisher, Function<T, String> data2LogPayload){
        return publisher
                .doOnNext(x -> logTiming(flowName, startTime, data2LogPayload!=null? data2LogPayload.apply(x) : ""));
    }

    public static <T> Flux<T> logTimingFinally(String flowName, Flux<T> publisher, String logPayload){
        return logTimingFinally(flowName, System.currentTimeMillis(), publisher, logPayload);
    }
    public static <T> Flux<T> logTimingFinally(String flowName, long startTime, Flux<T> publisher, String logPayload){
        return publisher
                .doFinally(x -> logTiming(flowName, startTime, ObjectUtils.firstNonNull(logPayload, "")));
    }
//endregion

    public static void logTiming(String flowName, long startTime, String logPayload){
        log.info("[PERFORMANCE_LOG] [{}] Time occurred to perform business logic: {} ms {}", flowName, System.currentTimeMillis() - startTime, logPayload);
    }
}
