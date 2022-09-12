package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.trx.ReversalInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class Transaction2RewardTransactionDTOMapperTest {

    Transaction2RewardTransactionMapper transactionDTO2RewardTransactionMapper = new Transaction2RewardTransactionMapper();

    @Test
    void mapWithNullRewardTransaction() {
        // When
        RewardTransactionDTO result = transactionDTO2RewardTransactionMapper.apply(null);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void mapWithNotNullRewardTransaction() {
        // Given
        TransactionDTO trx = TransactionDTOFaker.mockInstance(0);
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        trx.setTrxChargeDate(trx.getTrxDate());
        trx.setEffectiveAmount(trx.getAmount());
        trx.setReversalInfo(new ReversalInfo());

        // When
        RewardTransactionDTO result = transactionDTO2RewardTransactionMapper.apply(trx);

        // Then
        Assertions.assertNotNull(result);

        assertCommonFieldsValues(trx, result);

        TestUtils.checkNotNullFields(result, "status", "initiatives", "initiativeRejectionReasons", "rewards");
    }

    public static void assertCommonFieldsValues(TransactionDTO trx, TransactionDTO result) {
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
        Assertions.assertSame(trx.getRejectionReasons(), result.getRejectionReasons());
        Assertions.assertSame(trx.getOperationTypeTranscoded(), result.getOperationTypeTranscoded());
    }

}