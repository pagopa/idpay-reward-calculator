package it.gov.pagopa.common.mongo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.data.mongodb.primary")
public record PrimaryMongoProperties(
    String uri,
    String database,
    PrimaryMongoProperties.Config config
) {

  public record Config(
      PrimaryMongoProperties.Config.ConnectionPoolSettings connectionPool
  ) {

    public record ConnectionPoolSettings(
        int maxSize,
        int minSize,
        long maxWaitTimeMS,
        long maxConnectionLifeTimeMS,
        long maxConnectionIdleTimeMS,
        int maxConnecting
    ) {

    }
  }
}