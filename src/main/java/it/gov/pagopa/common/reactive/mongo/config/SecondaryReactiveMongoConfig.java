package it.gov.pagopa.common.reactive.mongo.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import it.gov.pagopa.common.config.CustomReactiveMongoHealthIndicator;
import it.gov.pagopa.common.mongo.config.SecondaryMongoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.util.StringUtils;

/**
 * Configurazione per un secondo database Mongo reattivo. Abilitata se la property
 * spring.data.mongodb.secondary.enabled=true
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.data.mongodb.secondary", name = "enabled", havingValue = "true")
@EnableReactiveMongoRepositories(
    basePackages = "it.gov.pagopa.reward.connector.repository.secondary",
    reactiveMongoTemplateRef = "secondaryReactiveMongoTemplate"
)
@Slf4j
public class SecondaryReactiveMongoConfig {

  private final SecondaryMongoProperties mongoConfig;

  public SecondaryReactiveMongoConfig(SecondaryMongoProperties mongoConfig) {
    this.mongoConfig = mongoConfig;
  }

  /**
   * Crea un {@link MongoClient} separato per il database secondario.
   */
  @Bean(name = "secondaryMongoClient")
  public MongoClient secondaryMongoClient(
      @Autowired(required = false) MongoClientSettingsBuilderCustomizer customizer) {
    if (!StringUtils.hasText(mongoConfig.uri()) || !StringUtils.hasText(mongoConfig.database())) {
      throw new IllegalStateException(
          "Secondary MongoDB enabled but uri/database not configured (spring.data.mongodb.secondary.uri / .database)");
    }
    log.info("Initializing secondary MongoClient for database {} at uri {}", mongoConfig.database(),
        maskConnString(mongoConfig.uri()));
    MongoClientSettings.Builder builder = MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString(mongoConfig.uri()));

    if (customizer != null) {
      customizer.customize(builder);
    }
    return MongoClients.create(builder.build());
  }

  @Bean(name = "secondaryReactiveMongoTemplate")
  public ReactiveMongoTemplate secondaryReactiveMongoTemplate(
      @Qualifier("secondaryMongoClient") MongoClient mongoClient) {
    log.info("Creating secondaryReactiveMongoTemplate for database {}", mongoConfig.database());
      return new ReactiveMongoTemplate(
              new SimpleReactiveMongoDatabaseFactory(mongoClient, properties.database()),
              (MongoConverter) mongoCustomConversions);
  }

  @Bean(name = "secondaryMongoHealthIndicator")
  public CustomReactiveMongoHealthIndicator secondaryMongoHealthIndicator(
      @Qualifier("secondaryReactiveMongoTemplate") ReactiveMongoTemplate reactiveMongoTemplate) {
    return new CustomReactiveMongoHealthIndicator(reactiveMongoTemplate);
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
      return uri; // fallback
    }
  }
}
