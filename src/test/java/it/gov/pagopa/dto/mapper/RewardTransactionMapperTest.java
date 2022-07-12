package it.gov.pagopa.dto.mapper;

import it.gov.pagopa.dto.RewardTransactionDTO;
import it.gov.pagopa.model.RewardTransaction;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class RewardTransactionMapperTest {

    RewardTransactionMapper rewardTransactionMapper = new RewardTransactionMapper();

    @Test
    void mapWithNullRewardTransaction() {
        // When
        RewardTransactionDTO result = rewardTransactionMapper.map(null);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void mapWithNotNullRewardTransaction(){
        // Given
        RewardTransaction rewardTransaction = new RewardTransaction();

        // When
        RewardTransactionDTO result = rewardTransactionMapper.map(rewardTransaction);

        // Then
        Assertions.assertNotNull(result);
    }
}