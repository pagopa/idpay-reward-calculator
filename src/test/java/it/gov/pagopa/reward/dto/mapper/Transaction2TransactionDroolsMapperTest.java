package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Transaction2TransactionDroolsMapperTest {

    Transaction2TransactionDroolsMapper transaction2TransactionDroolsMapper = new Transaction2TransactionDroolsMapper();

    @Test
    void mapWithNullTransactionDTO() {
        // When
        TransactionDroolsDTO result = transaction2TransactionDroolsMapper.apply(null);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void mapWithNotNullTransactionDTO(){
        // Given
        TransactionDTO trx = new TransactionDTO();

        // When
        TransactionDroolsDTO result = transaction2TransactionDroolsMapper.apply(trx);

        //Then
        Assertions.assertNotNull(result);

        TestUtils.checkNotNullFields(result);
    }
}