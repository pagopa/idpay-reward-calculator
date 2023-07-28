package it.gov.pagopa.common.reactive.mongo.retry;

import it.gov.pagopa.common.reactive.web.ReactiveRequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
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
public class MongoRequestRateTooLargeRetryWhenNotControllerAspect {

    @Value("${mongo.request-rate-too-large.max-retry:3}")
    private int maxRetry;
    @Value("${mongo.request-rate-too-large.max-millis-elapsed:0}")
    private int maxMillisElapsed;

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

        return Mono.deferContextual(ctx -> {
            if (isNotControllerContext(ctx)) {
                return MongoRequestRateTooLargeRetryer.withRetry(out, maxRetry, maxMillisElapsed);
            } else {
                return out;
            }
        });
    }

    @Around("inRepositoryClass() && returnFlux()")
    public Object decorateFluxRepositoryMethods(ProceedingJoinPoint pjp) throws Throwable {
        @SuppressWarnings("unchecked") // only with Flux the compiler return error when using wildcard, so here we are using Object
        Flux<Object> out = (Flux<Object>) pjp.proceed();

        return Flux.deferContextual(ctx -> {
            if (isNotControllerContext(ctx)) {
                return MongoRequestRateTooLargeRetryer.withRetry(out, maxRetry, maxMillisElapsed);
            } else {
                return out;
            }
        });
    }

    private static boolean isNotControllerContext(ContextView ctx) {
        return ctx.getOrEmpty(ReactiveRequestContextHolder.CONTEXT_KEY).isEmpty();
    }
}
