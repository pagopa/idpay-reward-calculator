package it.gov.pagopa.common.config;

import org.bson.Document;
import org.springframework.boot.health.contributor.AbstractReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

public class CustomReactiveMongoHealthIndicator extends AbstractReactiveHealthIndicator {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public CustomReactiveMongoHealthIndicator(ReactiveMongoTemplate reactiveMongoTemplate) {
        Assert.notNull(reactiveMongoTemplate, "ReactiveMongoTemplate must not be null");
        super("Mongo health check failed");
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    @Override
    protected Mono<Health> doHealthCheck(Health.Builder builder)  {
        Mono<Document> buildInfo = this.reactiveMongoTemplate.executeCommand("{ isMaster: 1 }");
        return buildInfo.map(document -> builderUp(builder, document));
    }
    private Health builderUp(Health.Builder builder, Document document) {
        return builder.up().withDetail("maxWireVersion", document.getInteger("maxWireVersion")).build();
    }
}
