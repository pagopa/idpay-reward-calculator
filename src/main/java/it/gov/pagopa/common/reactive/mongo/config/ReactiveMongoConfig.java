package it.gov.pagopa.common.reactive.mongo.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import it.gov.pagopa.common.mongo.config.PrimaryMongoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

  public ReactiveMongoConfig(PrimaryMongoProperties properties) {
    this.properties = properties;
  }

  @Primary
  @Bean(name = "reactiveMongoClient")
  public MongoClient mongoClient(
      @Autowired(required = false) MongoClientSettingsBuilderCustomizer customizer) {
    if (!StringUtils.hasText(properties.uri()) || !StringUtils.hasText(
        properties.database())) {
      throw new IllegalStateException(
          "MongoDB enabled but uri/database not configured (spring.data.mongodb.primary.uri / .database)");
    }
    log.info("Initializing MongoClient for database {} at uri {}", properties.database(),
        maskConnString(properties.uri()));
    MongoClientSettings.Builder builder = MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString(properties.uri()));
    if (customizer != null) {
      customizer.customize(builder);
    }
    return MongoClients.create(builder.build());
  }

    @Bean(name = "reactiveMongoTemplate")
    public ReactiveMongoTemplate reactiveMongoTemplate(
            @Qualifier("reactiveMongoClient") MongoClient mongoClient,
            MongoCustomConversions customConversions) {
        log.info("Creating ReactiveMongoTemplate for database {}", properties.database());

        SimpleReactiveMongoDatabaseFactory factory =
                new SimpleReactiveMongoDatabaseFactory(mongoClient, properties.database());

        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());

        DefaultReactiveDbRefResolver dbRefResolver = new DefaultReactiveDbRefResolver(factory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
        converter.setCustomConversions(customConversions);
        try {
            converter.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MappingMongoConverter", e);
        }

        return new ReactiveMongoTemplate(factory, (MongoConverter) converter);
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
}
