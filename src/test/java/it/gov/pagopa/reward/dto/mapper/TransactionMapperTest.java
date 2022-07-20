package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.RewardTransaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TransactionMapperTest {

    TransactionMapper transactionMapper = new TransactionMapper();

    @Test
    void mapWithNullTransactionDTO() {
        // When
        RewardTransaction result = transactionMapper.map(null);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void mapWithNotNullTransactionDTO(){
        // Given
        TransactionDTO trx = new TransactionDTO();

        // When
        RewardTransaction result = transactionMapper.map(trx);

        //Then
        Assertions.assertNotNull(result);
    }
}