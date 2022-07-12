package it.gov.pagopa.service.reward.filter;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.service.reward.filter.MccTransactionFilter;
import it.gov.pagopa.service.reward.filter.TransactionFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@Slf4j
class MccTransactionFilterTest {
    Set<String> mccExcluded = new HashSet<>(Arrays.asList("4784","6010","6011","7995","9222","9311"));

    @Test
    void testMCCValid() {
        // Given
        TransactionDTO trx = Mockito.spy(TransactionDTO.class);
        TransactionFilter filter = new MccTransactionFilter(mccExcluded);
        Mockito.when(trx.getMcc()).thenReturn("3333");

        // When
        boolean result = filter.test(trx);

        // Then
        Assertions.assertTrue(result);

    }

    @Test
    void testMccNotValid(){
        // Given
        TransactionDTO trx = Mockito.spy(TransactionDTO.class);
        TransactionFilter filter = new MccTransactionFilter(mccExcluded);
        Mockito.when(trx.getMcc()).thenReturn("6010");

        // When
        boolean result = filter.test(trx);

        // Then
        Assertions.assertFalse(result);
    }
}