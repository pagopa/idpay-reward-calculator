package it.gov.pagopa.common.reactive.web;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class ReactiveRequestContextHolder {
    private ReactiveRequestContextHolder(){}

    public static final Class<ServerWebExchange> CONTEXT_KEY = ServerWebExchange.class;

    public static Mono<ServerWebExchange> getRequest() {
        return Mono.deferContextual(ctx -> Mono.just(ctx.get(CONTEXT_KEY)));
    }
}