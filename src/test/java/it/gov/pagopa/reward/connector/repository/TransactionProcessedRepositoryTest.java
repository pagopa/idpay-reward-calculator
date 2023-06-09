package it.gov.pagopa.reward.connector.repository;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.temporal.ChronoUnit;
import java.util.List;

class TransactionProcessedRepositoryTest extends BaseIntegrationTest {

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
