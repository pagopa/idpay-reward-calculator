package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.common.mongo.config.MongoConfig;
import it.gov.pagopa.common.mongo.singleinstance.AutoConfigureSingleInstanceMongodb;
import it.gov.pagopa.common.reactive.mongo.config.ReactiveMongoConfig;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.temporal.ChronoUnit;
import java.util.List;

@TestPropertySource(properties = {
        "de.flapdoodle.mongodb.embedded.version=4.2.24",

        "spring.data.mongodb.database=idpay",
        "spring.data.mongodb.config.connectionPool.maxSize: 100",
        "spring.data.mongodb.config.connectionPool.minSize: 0",
        "spring.data.mongodb.config.connectionPool.maxWaitTimeMS: 120000",
        "spring.data.mongodb.config.connectionPool.maxConnectionLifeTimeMS: 0",
        "spring.data.mongodb.config.connectionPool.maxConnectionIdleTimeMS: 120000",
        "spring.data.mongodb.config.connectionPool.maxConnecting: 2",
})
@ExtendWith(SpringExtension.class)
@AutoConfigureSingleInstanceMongodb
@ContextConfiguration(classes = {TransactionProcessedRepository.class,
        ReactiveMongoConfig.class,
        Transaction2RewardTransactionMapper.class,
        MongoConfig.class})
class TransactionProcessedRepositoryTest {

    private static final String TEST_TRX = "TEST_TRX";

    @Autowired
    private TransactionProcessedRepository repository;

    @Autowired
    private Transaction2RewardTransactionMapper rewardTransactionMapper;

    @AfterEach
    void clearData(){
        repository.deleteAllById(List.of(TEST_TRX)).block();
    }

    @Test
    void testPolymorphismUsingReward(){
        RewardTransactionDTO rewardDto = rewardTransactionMapper.apply(TransactionDTOFaker.mockInstance(1));
        rewardDto.setId(TEST_TRX);

        testPolymorphism(rewardDto);
    }

    @Test
    void testPolymorphismUsingTransactionProcessed(){
        TransactionProcessed transactionProcessed = TransactionProcessedFaker.mockInstance(1);
        transactionProcessed.setId(TEST_TRX);

        testPolymorphism(transactionProcessed);
    }

    private void testPolymorphism(BaseTransactionProcessed entity){
        // When saving
        BaseTransactionProcessed resultSave = repository.save(entity).block();

        // Then
        checkResult(entity, resultSave);

        // When using findById
        BaseTransactionProcessed resultFindById = repository.findById(entity.getId()).block();

        // Then
        checkResult(entity, resultFindById);
    }

    private static void checkResult(BaseTransactionProcessed entity, BaseTransactionProcessed result) {
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.getClass(), entity.getClass());
        result.setElaborationDateTime(result.getElaborationDateTime().truncatedTo(ChronoUnit.MILLIS));
        Assertions.assertEquals(result, entity);
    }
}
