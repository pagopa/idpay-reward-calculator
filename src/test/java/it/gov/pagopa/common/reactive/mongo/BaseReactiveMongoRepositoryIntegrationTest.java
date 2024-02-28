package it.gov.pagopa.common.reactive.mongo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import it.gov.pagopa.common.mongo.MongoTest;
import it.gov.pagopa.common.mongo.MongoTestUtilitiesService;
import it.gov.pagopa.common.mongo.config.MongoConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;

import java.util.List;
import java.util.Map;

@MongoTest
class BaseReactiveMongoRepositoryIntegrationTest {

    static {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
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
    }

    @Autowired
    private DummySpringRepository repository;

    private static final List<String> ID_TEST_ENTITIES = List.of("ID", "ID2");

    @BeforeEach
    void initTestData(){
        ID_TEST_ENTITIES.forEach(this::storeTestData);
    }

    private void storeTestData(String idTestEntity) {
        DummySpringRepository.DummyMongoCollection testData = new DummySpringRepository.DummyMongoCollection();
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

        DummySpringRepository.DummyMongoCollection result = repository.findById(ID_TEST_ENTITIES.get(0)).block();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(ID_TEST_ENTITIES.get(0), result.getId());

        Assertions.assertNull(repository.findById("DUMMYID").block());

        List<Map.Entry<MongoTestUtilitiesService.MongoCommand, Long>> commands = MongoTestUtilitiesService.stopAndGetMongoCommands();
        Assertions.assertEquals(1, commands.size());
        Assertions.assertEquals("{\"find\": \"beneficiary_rule\", \"filter\": {\"_id\": \"VALUE\"}, \"$db\": \"idpay\"}", commands.get(0).getKey().getCommand());
        Assertions.assertEquals(2L, commands.get(0).getValue());
    }
}
