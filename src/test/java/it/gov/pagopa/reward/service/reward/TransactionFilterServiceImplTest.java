package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.service.reward.filter.TransactionFilter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;


@ExtendWith(MockitoExtension.class)
@Slf4j
class TransactionFilterServiceImplTest {


    @Test
    void testFilterFalse() {
        // Given
        TransactionFilter filterMock1 = configureTransactionFilterMock(true);
        TransactionFilter filterMock2 = configureTransactionFilterMock(true);
        TransactionFilter filterMock3 = configureTransactionFilterMock(false);
        TransactionFilter filterMock4 = Mockito.mock(TransactionFilter.class);
        List<TransactionFilter> filterMocks = Arrays.asList(filterMock1, filterMock2, filterMock3, filterMock4);
        TransactionFilterService transactionFilterService = new TransactionFilterServiceImpl(filterMocks);

        TransactionDTO trx = Mockito.mock(TransactionDTO.class);

        // When
        Boolean result = transactionFilterService.filter(trx);

        // Then
        Assertions.assertFalse(result);

        Mockito.verify(filterMock1).test(Mockito.same(trx));
        Mockito.verify(filterMock2).test(Mockito.same(trx));
        Mockito.verify(filterMock3).test(Mockito.same(trx));

        Mockito.verify(filterMock4,Mockito.never()).test(Mockito.any());

        Mockito.verifyNoMoreInteractions(filterMock1, filterMock2, filterMock3, filterMock4);

    }

    @Test
    void testFilterTrue() {
        // Given
        TransactionFilter filterMock1 = configureTransactionFilterMock(true);
        TransactionFilter filterMock2 = configureTransactionFilterMock(true);
        TransactionFilter filterMock3 = configureTransactionFilterMock(true);
        TransactionFilter filterMock4 = configureTransactionFilterMock(true);
        List<TransactionFilter> filterMocks = Arrays.asList(filterMock1,filterMock2, filterMock3, filterMock4);

        TransactionFilterService transactionFilterService = new TransactionFilterServiceImpl(filterMocks);

        TransactionDTO trx = Mockito.mock(TransactionDTO.class);

        // When
        boolean result = transactionFilterService.filter(trx);

        // Then
        Assertions.assertTrue(result);

        Mockito.verify(filterMock1).test(Mockito.same(trx));
        Mockito.verify(filterMock2).test(Mockito.same(trx));
        Mockito.verify(filterMock3).test(Mockito.same(trx));
        Mockito.verify(filterMock4).test(Mockito.same(trx));

        Mockito.verifyNoMoreInteractions(filterMock1, filterMock2, filterMock3, filterMock4);

    }

    private TransactionFilter configureTransactionFilterMock(boolean expectedReturn) {
        TransactionFilter filterMock = Mockito.mock(TransactionFilter.class);
        Mockito.when(filterMock.test(Mockito.any())).thenReturn(expectedReturn);
        return filterMock;
    }
}