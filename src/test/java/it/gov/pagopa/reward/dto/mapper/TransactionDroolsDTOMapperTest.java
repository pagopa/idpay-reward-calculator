package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class TransactionDroolsDTOMapperTest {

    TransactionDroolsDTO2RewardTransactionMapper transactionDroolsDTO2RewardTransactionMapper = new TransactionDroolsDTO2RewardTransactionMapper();

    @Test
    void mapWithNullRewardTransaction() {
        // When
        RewardTransactionDTO result = transactionDroolsDTO2RewardTransactionMapper.apply(null);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void mapWithNotNullRewardTransaction(){
        // Given
        TransactionDroolsDTO transactionDroolsDTO = new TransactionDroolsDTO();

        // When
        RewardTransactionDTO result = transactionDroolsDTO2RewardTransactionMapper.apply(transactionDroolsDTO);

        // Then
        Assertions.assertNotNull(result);

        TestUtils.checkNotNullFields(result);
    }
}