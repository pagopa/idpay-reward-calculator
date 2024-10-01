package it.gov.pagopa.common.config;

import org.bson.Document;
import org.springframework.boot.actuate.data.mongo.MongoReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;

public class CustomMongoHealthIndicator extends MongoReactiveHealthIndicator {

    private final ReactiveMongoTemplate mongoTemplate;

    public CustomMongoHealthIndicator(ReactiveMongoTemplate mongoTemplate) {
        super(mongoTemplate);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    protected Mono<Health> doHealthCheck(Health.Builder builder)  {
        Mono<Document> buildInfo = this.mongoTemplate.executeCommand("{ isMaster: 1 }");
        return buildInfo.map(document -> builderUp(builder, document));
    }
    private Health builderUp(Health.Builder builder, Document document) {
        return builder.up().withDetail("maxWireVersion", document.getInteger("maxWireVersion")).build();
    }
}
