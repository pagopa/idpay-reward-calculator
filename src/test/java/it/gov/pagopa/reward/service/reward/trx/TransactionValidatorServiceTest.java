package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
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
        Assertions.assertEquals(TestUtils.bigDecimalValue(2), result.getAmount());
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

    @Test
    void testAmountCentsZero(){
        testInvalidAmountCents(0L);
    }
    @Test
    void testAmountCentsNegative(){
        testInvalidAmountCents(-1L);
    }
    void testInvalidAmountCents(Long invalidAmountCents){
        // Given
        final TransactionDTO trxZero = TransactionDTOFaker.mockInstance(1);
        trxZero.setAmountCents(invalidAmountCents);

        // When
        final TransactionDTO result = transactionValidatorService.validate(trxZero);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trxZero, result);
        Assertions.assertEquals(List.of("INVALID_AMOUNT"), trxZero.getRejectionReasons());
    }

    @Test
    void testAmountCents(){
        // Given
        final TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(1)
                .amountCents(2_00L)
                .build();

        // When
        final TransactionDTO result = transactionValidatorService.validate(trx);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trx, result);
        Assertions.assertEquals(TestUtils.bigDecimalValue(2), result.getAmount());
        Assertions.assertEquals(Collections.emptyList(), trx.getRejectionReasons());
    }

    @Test
    void testAmountCentsAndAmount(){
        // Given
        final TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(1)
                .amountCents(2_00L)
                .amount(TestUtils.bigDecimalValue(2))
                .build();

        // When
        final TransactionDTO result = transactionValidatorService.validate(trx);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trx, result);
        Assertions.assertEquals(TestUtils.bigDecimalValue(2), result.getAmount());
        Assertions.assertEquals(Collections.emptyList(), trx.getRejectionReasons());
    }

    @Test
    void testAmountCentsAndAmountOverride(){
        // Given
        final TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(1)
                .amountCents(2_00L)
                .amount(TestUtils.bigDecimalValue(200))
                .build();

        // When
        final TransactionDTO result = transactionValidatorService.validate(trx);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(trx, result);
        Assertions.assertEquals(TestUtils.bigDecimalValue(2), result.getAmount());
        Assertions.assertEquals(Collections.emptyList(), trx.getRejectionReasons());
    }
}
