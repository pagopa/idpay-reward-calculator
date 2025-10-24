package it.gov.pagopa.common.mongo.config;

import com.mongodb.lang.NonNull;
import it.gov.pagopa.common.utils.CommonConstants;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.bson.types.Decimal128;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class MongoConfig {

  @ConfigurationProperties(prefix = "spring.data.mongodb.secondary")
  public record SecondaryMongoProperties(
      String uri,
      String database
  ) {

  }

  @Bean
  public MongoClientSettingsBuilderCustomizer customizer(
      PrimaryMongoProperties primaryMongoProperties) {
    return builder -> builder.applyToConnectionPoolSettings(
        connectionPool -> {
          connectionPool.maxSize(primaryMongoProperties.config().connectionPool().maxSize());
          connectionPool.minSize(primaryMongoProperties.config().connectionPool().minSize());
          connectionPool.maxWaitTime(primaryMongoProperties.config().connectionPool().maxWaitTimeMS(),
              TimeUnit.MILLISECONDS);
          connectionPool.maxConnectionLifeTime(
              primaryMongoProperties.config().connectionPool().maxConnectionLifeTimeMS(),
              TimeUnit.MILLISECONDS);
          connectionPool.maxConnectionIdleTime(
              primaryMongoProperties.config().connectionPool().maxConnectionIdleTimeMS(),
              TimeUnit.MILLISECONDS);
          connectionPool.maxConnecting(primaryMongoProperties.config().connectionPool().maxConnecting());
        });
  }

  @Bean
  public MongoCustomConversions mongoCustomConversions() {
    return new MongoCustomConversions(Arrays.asList(
        // BigDecimal support
        new BigDecimalDecimal128Converter(),
        new Decimal128BigDecimalConverter(),

        // OffsetDateTime support
        new OffsetDateTimeWriteConverter(),
        new OffsetDateTimeReadConverter()
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

  @WritingConverter
  public static class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {

    @Override
    public Date convert(OffsetDateTime offsetDateTime) {
      return Date.from(offsetDateTime.toInstant());
    }
  }

  @ReadingConverter
  public static class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(Date date) {
      return date.toInstant().atZone(CommonConstants.ZONEID).toOffsetDateTime();
    }
  }
}
