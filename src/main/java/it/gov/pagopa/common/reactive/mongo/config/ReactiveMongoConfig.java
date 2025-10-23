package it.gov.pagopa.common.reactive.mongo.config;

import com.mongodb.reactivestreams.client.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@EnableReactiveMongoRepositories(
    basePackages = "it.gov.pagopa.reward.connector.repository.primary",
    reactiveMongoTemplateRef = "reactiveMongoTemplate"
)
@Configuration
public class ReactiveMongoConfig {

  @Bean(name = "reactiveMongoTemplate")
  public ReactiveMongoTemplate reactiveMongoTemplate(
      MongoClient mongoClient,
      @Value("${spring.data.mongodb.database}") String database,
      MongoCustomConversions conversions
  ) {
    return new ReactiveMongoTemplate(mongoClient, database);
  }

}
