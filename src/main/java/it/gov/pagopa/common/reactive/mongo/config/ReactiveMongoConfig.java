package it.gov.pagopa.common.reactive.mongo.config;

import it.gov.pagopa.common.reactive.mongo.ReactiveMongoRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories(
        basePackages = "it.gov.pagopa",
        repositoryBaseClass = ReactiveMongoRepositoryImpl.class
)
public class ReactiveMongoConfig {
}
