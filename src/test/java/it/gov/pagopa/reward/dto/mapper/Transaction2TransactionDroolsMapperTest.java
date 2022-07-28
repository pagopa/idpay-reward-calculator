package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTOFaker;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

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
        TransactionDTO trx = TransactionDTOFaker.mockInstance(0);

        // When
        TransactionDroolsDTO result = transaction2TransactionDroolsMapper.apply(trx);

        //Then
        Assertions.assertNotNull(result);

        assertCommonFieldValues(trx, result);

        Assertions.assertEquals(new ArrayList<>(), result.getRejectionReasons());
        Assertions.assertEquals(new HashMap<>(), result.getInitiativeRejectionReasons());
        Assertions.assertNull(result.getInitiatives());
        Assertions.assertEquals(new HashMap<>(), result.getRewards());

        TestUtils.checkNotNullFields(result,"initiatives");
    }

    private void assertCommonFieldValues(TransactionDTO trx, TransactionDroolsDTO result) {
        Assertions.assertSame(trx.getIdTrxAcquirer(), result.getIdTrxAcquirer());
        Assertions.assertSame(trx.getAcquirerCode(), result.getAcquirerCode());
        Assertions.assertSame(trx.getTrxDate(), result.getTrxDate());
        Assertions.assertSame(trx.getHpan(), result.getHpan());
        Assertions.assertSame(trx.getOperationType(), result.getOperationType());
        Assertions.assertSame(trx.getCircuitType(), result.getCircuitType());
        Assertions.assertSame(trx.getIdTrxIssuer(), result.getIdTrxIssuer());
        Assertions.assertSame(trx.getCorrelationId(), result.getCorrelationId());
        Assertions.assertSame(trx.getAmount(), result.getAmount());
        Assertions.assertSame(trx.getAmountCurrency(), result.getAmountCurrency());
        Assertions.assertSame(trx.getMcc(), result.getMcc());
        Assertions.assertSame(trx.getAcquirerId(), result.getAcquirerId());
        Assertions.assertSame(trx.getMerchantId(), result.getMerchantId());
        Assertions.assertSame(trx.getTerminalId(), result.getTerminalId());
        Assertions.assertSame(trx.getBin(), result.getBin());
        Assertions.assertSame(trx.getSenderCode(), result.getSenderCode());
        Assertions.assertSame(trx.getFiscalCode(), result.getFiscalCode());
        Assertions.assertSame(trx.getVat(), result.getVat());
        Assertions.assertSame(trx.getPosType(), result.getPosType());
        Assertions.assertSame(trx.getPar(), result.getPar());
    }
}