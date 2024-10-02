package it.gov.pagopa.common.mongo.config;

import it.gov.pagopa.common.config.CustomReactiveMongoHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

@Configuration
public class MongoHealthConfig {
    @Bean
    public CustomReactiveMongoHealthIndicator customMongoHealthIndicator(ReactiveMongoTemplate reactiveMongoTemplate) {
        return new CustomReactiveMongoHealthIndicator(reactiveMongoTemplate);
    }
}

