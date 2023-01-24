package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionProcessedFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TransactionProcessedServiceImplTest {

    @Test
    void checkDuplicateTransactionsOk() {
        // Given
        TransactionProcessedRepository transactionProcessedRepositoryMock = Mockito.mock(TransactionProcessedRepository.class);
        TransactionProcessedService transactionProcessedService = new TransactionProcessedServiceImpl(operationTypeHandlerService, new Transaction2TransactionProcessedMapper(), transactionProcessedRepositoryMock, trxNotifierService);

        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);

        Mockito.when(transactionProcessedRepositoryMock.findById(trx.getId())).thenReturn(Mono.empty());

        // When
        TransactionDTO result = transactionProcessedService.checkDuplicateTransactions(trx).block();

        // Then
        Assertions.assertSame(trx, result);
    }

    @Test
    void checkDuplicateTransactionsKo() {
        // Given
        TransactionProcessedRepository transactionProcessedRepositoryMock = Mockito.mock(TransactionProcessedRepository.class);
        TransactionProcessedService transactionProcessedService = new TransactionProcessedServiceImpl(operationTypeHandlerService, new Transaction2TransactionProcessedMapper(), transactionProcessedRepositoryMock, trxNotifierService);

        TransactionDTO trx = TransactionDTOFaker.mockInstance(1);

        TransactionProcessed trxDuplicate = TransactionProcessedFaker.mockInstance(1);
        Mockito.when(transactionProcessedRepositoryMock.findById(trx.getId())).thenReturn(Mono.just(trxDuplicate));

        // When
        TransactionDTO result = transactionProcessedService.checkDuplicateTransactions(trx).block();

        // Then
        Assertions.assertNull(result);
    }
}