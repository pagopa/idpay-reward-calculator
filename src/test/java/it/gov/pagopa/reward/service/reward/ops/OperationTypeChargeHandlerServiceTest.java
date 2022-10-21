package it.gov.pagopa.reward.service.reward.ops;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class OperationTypeChargeHandlerServiceTest {

    private final OperationTypeChargeHandlerService operationTypeChargeHandlerService = new OperationTypeChargeHandlerServiceImpl();

    @Test
    void test(){
        // When
        final TransactionDTO result = operationTypeChargeHandlerService.handleChargeOperation(TransactionDTOFaker.mockInstance(0)).block();

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(OperationType.CHARGE, result.getOperationTypeTranscoded());
        Assertions.assertEquals(result.getTrxDate(), result.getTrxChargeDate());
        Assertions.assertEquals(result.getAmount(), result.getEffectiveAmount());
        Assertions.assertNull(result.getRefundInfo());
        Assertions.assertEquals(Collections.emptyList(), result.getRejectionReasons());
    }
}
