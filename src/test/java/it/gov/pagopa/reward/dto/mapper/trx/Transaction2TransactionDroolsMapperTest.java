package it.gov.pagopa.reward.dto.mapper.trx;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.common.utils.TestUtils;
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
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        trx.setTrxChargeDate(trx.getTrxDate());
        trx.setAmountCents(trx.getAmount().longValue());
        trx.setAmount(CommonUtilities.centsToEuro(trx.getAmountCents()));
        trx.setEffectiveAmount(trx.getAmount());
        trx.setRefundInfo(new RefundInfo());

        // When
        TransactionDroolsDTO result = transaction2TransactionDroolsMapper.apply(trx);

        //Then
        Assertions.assertNotNull(result);

        Transaction2RewardTransactionDTOMapperTest.assertCommonFieldsValues(trx, result);

        Assertions.assertEquals(new ArrayList<>(), result.getRejectionReasons());
        Assertions.assertEquals(new HashMap<>(), result.getInitiativeRejectionReasons());
        Assertions.assertNull(result.getInitiatives());
        Assertions.assertEquals(new HashMap<>(), result.getRewards());

        TestUtils.checkNotNullFields(result,"initiatives");
    }

}