package it.gov.pagopa.common.reactive.mongo.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.util.StringUtils;

@EnableReactiveMongoRepositories(
    basePackages = "it.gov.pagopa.reward.connector.repository.primary",
    reactiveMongoTemplateRef = "reactiveMongoTemplate"
)
@Configuration
@Slf4j
public class ReactiveMongoConfig {

  @Value("${spring.data.mongodb.primary.uri:}")
  private String uri;
  @Value("${spring.data.mongodb.primary.database:}")
  private String database;

  @Primary
  @Bean(name = "reactiveMongoClient")
  public MongoClient mongoClient(@Autowired(required = false) MongoClientSettingsBuilderCustomizer customizer) {
    if(!StringUtils.hasText(uri) || !StringUtils.hasText(database)) {
      throw new IllegalStateException("Secondary MongoDB abilitato ma uri/database non configurati (spring.data.mongodb.secondary.uri / .database)");
    }
//    log.info("Initializing secondary MongoClient for database {} at uri {}", database, maskConnString(secondaryUri));
    MongoClientSettings.Builder builder = MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString(uri));
    // riuso customizzazioni pool (stesse impostazioni del primario)
    if (customizer != null) {
      customizer.customize(builder);
    }
    return MongoClients.create(builder.build());
  }

  @Bean(name = "reactiveMongoTemplate")
  public ReactiveMongoTemplate reactiveMongoTemplate(@Qualifier("secondaryMongoClient") MongoClient mongoClient) {
    log.info("Creating ReactiveMongoTemplate for database {}", database);
    return new ReactiveMongoTemplate(new SimpleReactiveMongoDatabaseFactory(mongoClient, database));
  }
}
