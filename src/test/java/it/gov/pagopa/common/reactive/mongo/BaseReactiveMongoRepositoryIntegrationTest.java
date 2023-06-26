package it.gov.pagopa.common.reactive.mongo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.mongo.config.MongoConfig;
import it.gov.pagopa.common.reactive.mongo.config.ReactiveMongoConfig;
import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.model.CamelCaseAbbreviatingFieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentProperty;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.mongodb.repository.support.MappingMongoEntityInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

@TestPropertySource(properties = {
        "de.flapdoodle.mongodb.embedded.version=4.0.21",

        "spring.data.mongodb.database=idpay",
        "spring.data.mongodb.config.connectionPool.maxSize: 100",
        "spring.data.mongodb.config.connectionPool.minSize: 0",
        "spring.data.mongodb.config.connectionPool.maxWaitTimeMS: 120000",
        "spring.data.mongodb.config.connectionPool.maxConnectionLifeTimeMS: 0",
        "spring.data.mongodb.config.connectionPool.maxConnectionIdleTimeMS: 120000",
        "spring.data.mongodb.config.connectionPool.maxConnecting: 2",
})
@ExtendWith(SpringExtension.class)
@AutoConfigureDataMongo
@ContextConfiguration(classes = {BaseReactiveMongoRepositoryIntegrationTest.TestMongoRepositoryConfig.class, ReactiveMongoConfig.class, MongoTestUtilitiesService.TestMongoConfiguration.class, SimpleMeterRegistry.class})
class BaseReactiveMongoRepositoryIntegrationTest {

    static {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Document("beneficiary_rule")
    @Data
    public static class TestCollection {
        @Id
        private String id;
    }

    @TestConfiguration
    static class TestMongoRepositoryConfig extends MongoConfig {
        @Autowired
        private MongoMetricsCommandListener mongoMetricsCommandListener;

        @Override
        public MongoClientSettingsBuilderCustomizer customizer(MongoDbCustomProperties mongoDbCustomProperties) {
            return builder -> {
                super.customizer(mongoDbCustomProperties).customize(builder);
                builder.addCommandListener(mongoMetricsCommandListener);
            };
        }

        @Bean
        public TestRepository configureTestRepository(ReactiveMongoOperations mongoOperations) throws NoSuchFieldException {
            TypeInformation<TestCollection> testTypeInformation = TypeInformation.of(TestCollection.class);
            BasicMongoPersistentEntity<TestCollection> testPersistentEntity = new BasicMongoPersistentEntity<>(testTypeInformation);
            testPersistentEntity.addPersistentProperty(new BasicMongoPersistentProperty(
                    Property.of(testTypeInformation, TestCollection.class.getDeclaredField("id")),
                    testPersistentEntity,
                    new SimpleTypeHolder(Set.of(TestCollection.class), true),
                    new CamelCaseAbbreviatingFieldNamingStrategy()
            ));
            return new TestRepository(new MappingMongoEntityInformation<>(testPersistentEntity), mongoOperations);
        }
    }

    static class TestRepository extends ReactiveMongoRepositoryImpl<TestCollection, String> implements ReactiveMongoRepository<TestCollection, String> {

        public TestRepository(MongoEntityInformation<TestCollection, String> entityInformation, ReactiveMongoOperations mongoOperations) {
            super(entityInformation, mongoOperations);
        }
    }

    @Autowired
    private TestRepository repository;

    private static final List<String> ID_TEST_ENTITIES = List.of("ID", "ID2");

    @BeforeEach
    void initTestData(){
        ID_TEST_ENTITIES.forEach(this::storeTestData);
    }

    private void storeTestData(String idTestEntity) {
        TestCollection testData = new TestCollection();
        testData.setId(idTestEntity);
        repository.save(testData).block();
    }

    @AfterEach
    void clearTestData(){
        repository.deleteAllById(ID_TEST_ENTITIES).block();
    }

    @Test
    void testFindById() {
        MongoTestUtilitiesService.startMongoCommandListener();

        TestCollection result = repository.findById(ID_TEST_ENTITIES.get(0)).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ID_TEST_ENTITIES.get(0), result.getId());

        List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>> commands = MongoTestUtilitiesService.stopAndGetMongoCommands();
        Assertions.assertEquals(1, commands.size());
        Assertions.assertEquals("{\"find\": \"beneficiary_rule\", \"filter\": {\"_id\": \"VALUE\"}, \"$db\": \"idpay\"}", commands.get(0).getKey().getCommand());
        Assertions.assertEquals(1L, commands.get(0).getValue());
    }
}
