package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class OperationTypeHandlerServiceTest {

    @Mock private OperationTypeChargeHandlerService operationTypeChargeHandlerServiceMock;
    @Mock private OperationTypeRefundHandlerService operationTypeRefundHandlerServiceMock;

    private OperationTypeHandlerService operationTypeHandlerService;

    @BeforeEach
    public void init(){
        operationTypeHandlerService = new OperationTypeHandlerServiceImpl(Set.of("00"), Set.of("01"), operationTypeChargeHandlerServiceMock, operationTypeRefundHandlerServiceMock);
    }

    @Test
    void testCharge(){
        // Given
        Mockito.when(operationTypeChargeHandlerServiceMock.handleChargeOperation(Mockito.any())).thenAnswer(i-> Mono.just(i.getArgument(0)));

        TransactionDTO trxCharge = TransactionDTOFaker.mockInstance(0);
        trxCharge.setOperationType("00");

        // When
        final TransactionDTO result = operationTypeHandlerService.handleOperationType(trxCharge).block();

        // Then
        Assertions.assertSame(trxCharge, result);

        Mockito.verify(operationTypeChargeHandlerServiceMock).handleChargeOperation(Mockito.same(trxCharge));
        Mockito.verifyNoMoreInteractions(operationTypeChargeHandlerServiceMock, operationTypeRefundHandlerServiceMock);
    }

    @Test
    void testRefund(){
        // Given
        Mockito.when(operationTypeRefundHandlerServiceMock.handleRefundOperation(Mockito.any())).thenAnswer(i-> Mono.just(i.getArgument(0)));

        TransactionDTO trxRefund = TransactionDTOFaker.mockInstance(0);
        trxRefund.setOperationType("01");

        // When
        final TransactionDTO result = operationTypeHandlerService.handleOperationType(trxRefund).block();

        // Then
        Assertions.assertSame(trxRefund, result);

        Mockito.verify(operationTypeRefundHandlerServiceMock).handleRefundOperation(Mockito.same(trxRefund));
        Mockito.verifyNoMoreInteractions(operationTypeChargeHandlerServiceMock, operationTypeRefundHandlerServiceMock);
    }

    @Test
    void testInvalidOperationType(){
        // Given
        TransactionDTO trxInvalidOp = TransactionDTOFaker.mockInstance(0);
        trxInvalidOp.setOperationType("000");

        // When
        final TransactionDTO result = operationTypeHandlerService.handleOperationType(trxInvalidOp).block();

        // Then
        Assertions.assertSame(trxInvalidOp, result);
        Assertions.assertEquals(List.of("INVALID_OPERATION_TYPE"), trxInvalidOp.getRejectionReasons());

        Mockito.verifyNoInteractions(operationTypeChargeHandlerServiceMock, operationTypeRefundHandlerServiceMock);
    }
}
