package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

class TransactionValidatorServiceTest {

    private final TransactionValidatorService transactionValidatorService = new TransactionValidatorServiceImpl();

    @Test
    void testSuccessful(){
        // Given
        final TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(1)
                .amount(BigDecimal.valueOf(2_00))
                .build();

        // When
        final TransactionDTO result = transactionValidatorService.validate(trx);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trx, result);
        Assertions.assertEquals(0, BigDecimal.valueOf(2).compareTo(result.getAmount()));
        Assertions.assertEquals(Collections.emptyList(), trx.getRejectionReasons());
    }

    @Test
    void testAmountZero(){
        testInvalidAmount(BigDecimal.ZERO);
    }
    @Test
    void testAmountNegative(){
        testInvalidAmount(BigDecimal.ONE.negate());
    }
    void testInvalidAmount(BigDecimal invalidAmount){
        // Given
        final TransactionDTO trxZero = TransactionDTOFaker.mockInstance(1);
        trxZero.setAmount(invalidAmount);

        // When
        final TransactionDTO result = transactionValidatorService.validate(trxZero);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trxZero, result);
        Assertions.assertEquals(List.of("INVALID_AMOUNT"), trxZero.getRejectionReasons());
    }
}
