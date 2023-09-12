package it.gov.pagopa.common.reactive.web;

import lombok.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Service
public class ReactiveRequestContextFilter implements WebFilter {

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(ReactiveRequestContextHolder.CONTEXT_KEY, exchange));
    }
}