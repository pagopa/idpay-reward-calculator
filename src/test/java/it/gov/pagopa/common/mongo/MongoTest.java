package it.gov.pagopa.common.mongo;

import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import it.gov.pagopa.common.mongo.config.MongoConfig;
import it.gov.pagopa.common.reactive.mongo.config.SecondaryReactiveMongoConfig;
import it.gov.pagopa.common.mongo.singleinstance.AutoConfigureSingleInstanceMongodb;
import it.gov.pagopa.common.reactive.mongo.config.ReactiveMongoConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(SpringExtension.class)
@TestPropertySource(
        properties = {
                "de.flapdoodle.mongodb.embedded.version=4.2.24",

                "spring.data.mongodb.database=idpay",
                "spring.data.mongodb.config.connectionPool.maxSize: 100",
                "spring.data.mongodb.config.connectionPool.minSize: 0",
                "spring.data.mongodb.config.connectionPool.maxWaitTimeMS: 120000",
                "spring.data.mongodb.config.connectionPool.maxConnectionLifeTimeMS: 0",
                "spring.data.mongodb.config.connectionPool.maxConnectionIdleTimeMS: 120000",
                "spring.data.mongodb.config.connectionPool.maxConnecting: 2",
                "spring.data.mongodb.secondary.uri=mongodb://localhost:${spring.data.mongodb.port}",
                "spring.data.mongodb.secondary.database=idpay2",
                "spring.data.mongodb.secondary.enabled=true",
        })
@AutoConfigureSingleInstanceMongodb
@Import({MongoTestUtilitiesService.TestMongoConfiguration.class,
        ReactiveMongoConfig.class,
        SecondaryReactiveMongoConfig.class,
        SimpleMeterRegistry.class,
        MongoTest.MongoTestConfiguration.class})
public @interface MongoTest {
    @TestConfiguration
    class MongoTestConfiguration extends MongoConfig {
        @Autowired
        private MongoMetricsCommandListener mongoMetricsCommandListener;

        @Override
        public MongoClientSettingsBuilderCustomizer customizer(MongoDbCustomProperties mongoDbCustomProperties) {
            return builder -> {
                super.customizer(mongoDbCustomProperties).customize(builder);
                builder.addCommandListener(mongoMetricsCommandListener);
            };
        }
    }
}
