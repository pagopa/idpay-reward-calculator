package it.gov.pagopa.reward.config;

import com.mongodb.lang.NonNull;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import lombok.Setter;
import org.bson.types.Decimal128;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableReactiveMongoRepositories(basePackageClasses = DroolsRuleRepository.class)
public class DbConfig {

    @Configuration
    @ConfigurationProperties(prefix = "spring.data.mongodb.config")
    static class MongoDbCustomProperties {
        @Setter
        ConnectionPoolSettings connectionPool;

        @Setter
        static class ConnectionPoolSettings {
            int maxSize;
            int minSize;
            long maxWaitTimeMS;
            long maxConnectionLifeTimeMS;
            long maxConnectionIdleTimeMS;
            int maxConnecting;
        }

    }

    @Bean
    public MongoClientSettingsBuilderCustomizer customizer(MongoDbCustomProperties mongoDbCustomProperties) {
        return builder -> builder.applyToConnectionPoolSettings(
                connectionPool -> {
                    connectionPool.maxSize(mongoDbCustomProperties.connectionPool.maxSize);
                    connectionPool.minSize(mongoDbCustomProperties.connectionPool.minSize);
                    connectionPool.maxWaitTime(mongoDbCustomProperties.connectionPool.maxWaitTimeMS, TimeUnit.MILLISECONDS);
                    connectionPool.maxConnectionLifeTime(mongoDbCustomProperties.connectionPool.maxConnectionLifeTimeMS, TimeUnit.MILLISECONDS);
                    connectionPool.maxConnectionIdleTime(mongoDbCustomProperties.connectionPool.maxConnectionIdleTimeMS, TimeUnit.MILLISECONDS);
                    connectionPool.maxConnecting(mongoDbCustomProperties.connectionPool.maxConnecting);
                });
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new BigDecimalDecimal128Converter(),
                new Decimal128BigDecimalConverter()
        ));

    }

    @WritingConverter
    private static class BigDecimalDecimal128Converter implements Converter<BigDecimal, Decimal128> {

        @Override
        public Decimal128 convert(@NonNull BigDecimal source) {
            return new Decimal128(source);
        }
    }

    @ReadingConverter
    private static class Decimal128BigDecimalConverter implements Converter<Decimal128, BigDecimal> {

        @Override
        public BigDecimal convert(@NonNull Decimal128 source) {
            return source.bigDecimalValue();
        }

    }
}

