package it.gov.pagopa.dto.mapper;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.model.RewardTransaction;
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