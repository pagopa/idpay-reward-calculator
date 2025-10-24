package it.gov.pagopa.common.mongo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.data.mongodb.secondary")
public record SecondaryMongoProperties(
    String uri,
    String database
) {

}
