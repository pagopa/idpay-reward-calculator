package it.gov.pagopa.common.reactive.mongo.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import it.gov.pagopa.common.mongo.config.MongoConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
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

  private final PrimaryMongoProperties properties;

  public ReactiveMongoConfig(PrimaryMongoProperties properties,
      MongoConfig.PrimaryMongoProperties prop) {
    this.properties = properties;
  }

  @Primary
  @Bean(name = "reactiveMongoClient")
  public MongoClient mongoClient(
      @Autowired(required = false) MongoClientSettingsBuilderCustomizer customizer) {
    if (!StringUtils.hasText(properties.getUri()) || !StringUtils.hasText(
        properties.getDatabase())) {
      throw new IllegalStateException(
          "MongoDB enabled but uri/database not configured (spring.data.mongodb.primary.uri / .database)");
    }
    log.info("Initializing MongoClient for database {} at uri {}", properties.getDatabase(),
        maskConnString(properties.getUri()));
    MongoClientSettings.Builder builder = MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString(properties.getUri()));
    if (customizer != null) {
      customizer.customize(builder);
    }
    return MongoClients.create(builder.build());
  }

  @Bean(name = "reactiveMongoTemplate")
  public ReactiveMongoTemplate reactiveMongoTemplate(
      @Qualifier("reactiveMongoClient") MongoClient mongoClient) {
    log.info("Creating ReactiveMongoTemplate for database {}", properties.getDatabase());
    return new ReactiveMongoTemplate(
        new SimpleReactiveMongoDatabaseFactory(mongoClient, properties.getDatabase()));
  }

  private String maskConnString(String uri) {
    if (uri == null) {
      return null;
    }
    try {
      ConnectionString cs = new ConnectionString(uri);
      String user = cs.getUsername();
      if (user == null) {
        return uri;
      }
      return uri.replaceFirst(user + ":.*?@", user + ":***@");
    } catch (Exception e) {
      return uri;
    }
  }

  @Data
  @Configuration
  @ConfigurationProperties(prefix = "spring.data.mongodb.primary")
  public static class PrimaryMongoProperties {

    private String uri;
    private String database;
  }
}
