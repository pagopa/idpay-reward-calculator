package it.gov.pagopa.common.reactive.mongo.retry;

import it.gov.pagopa.common.reactive.web.ReactiveRequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@Configuration
@EnableAspectJAutoProxy
@Aspect
@Slf4j
public class MongoRequestRateTooLargeAutomaticRetryAspect {

    private final boolean enabledApi;
    private final long maxRetryApi;
    private final long maxMillisElapsedApi;
    private final boolean enabledBatch;
    private final long maxRetryBatch;
    private final long maxMillisElapsedBatch;

    public MongoRequestRateTooLargeAutomaticRetryAspect(
        @Value("${mongo.request-rate-too-large.api.enabled}") boolean enabledApi,
        @Value("${mongo.request-rate-too-large.api.max-retry:3}") long maxRetryApi,
        @Value("${mongo.request-rate-too-large.api.max-millis-elapsed:0}") long maxMillisElapsedApi,
        @Value("${mongo.request-rate-too-large.batch.enabled}") boolean enabledBatch,
        @Value("${mongo.request-rate-too-large.batch.max-retry}") long maxRetryBatch,
        @Value("${mongo.request-rate-too-large.batch.max-millis-elapsed}") long maxMillisElapsedBatch) {
        this.enabledApi = enabledApi;
        this.maxRetryApi = maxRetryApi;
        this.maxMillisElapsedApi = maxMillisElapsedApi;
        this.enabledBatch = enabledBatch;
        this.maxRetryBatch = maxRetryBatch;
        this.maxMillisElapsedBatch = maxMillisElapsedBatch;
    }

    @Pointcut("within(*..*Repository*)")
    public void inRepositoryClass() {
    }

    @Pointcut("execution(public reactor.core.publisher.Mono *(..))")
    public void returnMono() {
    }

    @Pointcut("execution(public reactor.core.publisher.Flux *(..))")
    public void returnFlux() {
    }

    @Around("inRepositoryClass() && returnMono()")
    public Object decorateMonoRepositoryMethods(ProceedingJoinPoint pjp) throws Throwable {
        Mono<?> out = (Mono<?>) pjp.proceed();

        return Mono.deferContextual(ctx -> decorateMethod(out, ctx));
    }

    @Around("inRepositoryClass() && returnFlux()")
    public Object decorateFluxRepositoryMethods(ProceedingJoinPoint pjp) throws Throwable {
        @SuppressWarnings("unchecked") // only with Flux the compiler return error when using wildcard, so here we are using Object
        Flux<Object> out = (Flux<Object>) pjp.proceed();

        return Flux.deferContextual(ctx -> decorateMethod(out, ctx));
    }

    private <T extends Publisher<?>> T decorateMethod(T out, ContextView ctx) {
        if (isNotControllerContext(ctx)) {
            if(enabledBatch) {
                return invokeWithRetry(out, maxRetryBatch, maxMillisElapsedBatch);
            }else {
                return out;
            }
        } else {
            if(enabledApi){
                return invokeWithRetry(out,  maxRetryApi, maxMillisElapsedApi);
            }else {
                return out;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Publisher<?>> T invokeWithRetry(T out, long maxRetry, long maxMillisElapsed) {
        if(out instanceof Mono<?> mono) {
            return (T) MongoRequestRateTooLargeRetryer.withRetry(mono, maxRetry,
                    maxMillisElapsed);
        } else {
            return (T) MongoRequestRateTooLargeRetryer.withRetry((Flux<?>) out, maxRetry,
                    maxMillisElapsed);
        }
    }

    private static boolean isNotControllerContext(ContextView ctx) {
        return ctx.getOrEmpty(ReactiveRequestContextHolder.CONTEXT_KEY).isEmpty();
    }
}
