package it.gov.pagopa.common.mongo.singleinstance;

import de.flapdoodle.embed.mongo.commands.MongodArguments;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.spring.autoconfigure.*;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import it.gov.pagopa.common.mongo.EmbeddedMongodbTestClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;

/**
 * Used to start just once Mongodb instance for the entire duration of the test, dropping the database at each new Spring Context
 */
@AutoConfiguration(
        before = {MongoAutoConfiguration.class, MongoReactiveAutoConfiguration.class}
)
public class SingleEmbeddedMongodbConfiguration extends EmbeddedMongoAutoConfiguration {

    private static MongodWrapper singleMongodWrapperInstance;

    private final static Constructor<ReactiveClientServerFactory> unprotectedReactiveClientServerFactoryConstructor;
    private final static Constructor<SyncClientServerFactory> unprotectedSyncClientServerFactoryConstructor;

    static {
        try {
            unprotectedReactiveClientServerFactoryConstructor = ReactiveClientServerFactory.class.getDeclaredConstructor(MongoProperties.class);
            unprotectedReactiveClientServerFactoryConstructor.setAccessible(true);

            unprotectedSyncClientServerFactoryConstructor = SyncClientServerFactory.class.getDeclaredConstructor(MongoProperties.class);
            unprotectedSyncClientServerFactoryConstructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot unprotect AbstractServerFactory constructor", e);
        }
    }

    private final EmbeddedMongodbTestClient embeddedMongodbTestClient;

    public SingleEmbeddedMongodbConfiguration(EmbeddedMongodbTestClient embeddedMongodbTestClient) {
        this.embeddedMongodbTestClient = embeddedMongodbTestClient;
    }

    @Bean
    @Override
    public Net net(ConfigurableApplicationContext context) throws IOException {
        if(SingleInstanceMongodWrapper.singleMongodNet!=null){
            ConfigurableEnvironment env = context.getEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource("embeddedMongoReusedProperties",
                    Map.of("spring.data.mongodb.port", SingleInstanceMongodWrapper.singleMongodNet.getPort())));
            super.net(context);

            String mongodbUrl = context.getEnvironment().getProperty("spring.data.mongodb.uri");
            Objects.requireNonNull(mongodbUrl);
            String dbName = StringUtils.substringAfterLast(mongodbUrl, "/");

            embeddedMongodbTestClient.dropDatabase(mongodbUrl, dbName);

            return SingleInstanceMongodWrapper.singleMongodNet;
        }else {
            return SingleInstanceMongodWrapper.singleMongodNet = super.net(context);
        }
    }

    @ConditionalOnClass(name = {
            "com.mongodb.reactivestreams.client.MongoClient",
            "org.springframework.data.mongodb.core.ReactiveMongoClientFactoryBean"
    })
    static class ReactiveClientServerWrapperConfig {
        ReactiveClientServerWrapperConfig() {}

        @Bean(
                initMethod = "start",
                destroyMethod = "stop"
        )
        @ConditionalOnMissingBean
        public MongodWrapper reactiveClientServerWrapper(IFeatureAwareVersion version, MongoProperties properties, Mongod mongod, MongodArguments mongodArguments) {
            return Objects.requireNonNullElseGet(
                    singleMongodWrapperInstance,
                    () -> createMongodWrapper(unprotectedReactiveClientServerFactoryConstructor, version, properties, mongod, mongodArguments));
        }

        @Bean
        @ConditionalOnMissingBean
        public EmbeddedMongodbTestClient embeddedMongodbTestClient() {
            try {
                return (EmbeddedMongodbTestClient) Class.forName("it.gov.pagopa.common.reactive.mongo.EmbeddedMongodbTestReactiveClient").getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException("Cannot create EmbeddedMongodbTestClient", e);
            }
        }
    }

    @ConditionalOnClass(name = {
            "com.mongodb.client.MongoClient",
            "org.springframework.data.mongodb.core.MongoClientFactoryBean"
    })
    static class SyncClientServerWrapperConfig {
        SyncClientServerWrapperConfig() {}

        @Bean(
                initMethod = "start",
                destroyMethod = "stop"
        )
        @ConditionalOnMissingBean
        public MongodWrapper syncClientServerWrapper(IFeatureAwareVersion version, MongoProperties properties, Mongod mongod, MongodArguments mongodArguments) {
            return Objects.requireNonNullElseGet(
                    singleMongodWrapperInstance,
                    () -> createMongodWrapper(unprotectedSyncClientServerFactoryConstructor, version, properties, mongod, mongodArguments));
        }

        @Bean
        @ConditionalOnMissingBean
        public EmbeddedMongodbTestClient embeddedMongodbTestClient() {
            try {
                return (EmbeddedMongodbTestClient) Class.forName("it.gov.pagopa.common.mongo.EmbeddedMongodbTestSyncClient").getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException("Cannot create EmbeddedMongodbTestClient", e);
            }
        }
    }

    private static MongodWrapper createMongodWrapper(Constructor<? extends AbstractServerFactory<?>> unprotectedSyncClientServerFactoryConstructor, IFeatureAwareVersion version, MongoProperties properties, Mongod mongod, MongodArguments mongodArguments) {
        try {
            return singleMongodWrapperInstance = new SingleInstanceMongodWrapper(unprotectedSyncClientServerFactoryConstructor.newInstance(properties).createWrapper(version, mongod, mongodArguments));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot call protected constructor", e);
        }
    }
}
