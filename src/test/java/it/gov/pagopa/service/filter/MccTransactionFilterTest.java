package it.gov.pagopa.service.filter;

import it.gov.pagopa.dto.TransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@Slf4j
class MccTransactionFilterTest {
    @Value("${app.filter.mccExcluded}")
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