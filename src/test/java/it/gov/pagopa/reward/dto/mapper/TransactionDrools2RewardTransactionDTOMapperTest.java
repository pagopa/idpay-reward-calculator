package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.test.fakers.TransactionDroolsDtoFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
class TransactionDrools2RewardTransactionDTOMapperTest {

    TransactionDroolsDTO2RewardTransactionMapper transactionDroolsDTO2RewardTransactionMapper = new TransactionDroolsDTO2RewardTransactionMapper();

    @Test
    void mapWithNullRewardTransaction() {
        // When
        RewardTransactionDTO result = transactionDroolsDTO2RewardTransactionMapper.apply(null);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void mapWithNotNullRewardTransaction() {
        // Given
        TransactionDroolsDTO trx = TransactionDroolsDtoFaker.mockInstance(0);

        trx.setRewards(Map.of("INITIATIVE1", new Reward(BigDecimal.ZERO, BigDecimal.ZERO, false)));
        trx.setRewards(Map.of("INITIATIVE2", new Reward(BigDecimal.TEN, BigDecimal.ZERO, true)));
        trx.setRewards(Map.of("INITIATIVE3", new Reward(BigDecimal.TEN, BigDecimal.ONE, true)));

        trx.setInitiatives(new ArrayList<>());

        // When
        RewardTransactionDTO result = transactionDroolsDTO2RewardTransactionMapper.apply(trx);

        // Then
        Assertions.assertNotNull(result);

        assertCommonFieldsValues(trx, result);

        Assertions.assertEquals("REWARDED", result.getStatus());

        TestUtils.checkNotNullFields(result);
    }

    private void assertCommonFieldsValues(TransactionDroolsDTO trx, RewardTransactionDTO result) {
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
        Assertions.assertSame(trx.getInitiativeRejectionReasons(), result.getInitiativeRejectionReasons());
        Assertions.assertSame(trx.getRewards(), result.getRewards());
    }

    @Test
    void testWithNoRewards() {
        // Given
        TransactionDroolsDTO trx = TransactionDroolsDtoFaker.mockInstance(0);
        trx.setRewards(Map.of("INITIATIVE1", new Reward(BigDecimal.ZERO, BigDecimal.ZERO, false)));
        trx.setRewards(Map.of("INITIATIVE2", new Reward(BigDecimal.TEN, BigDecimal.ZERO, true)));

        // When
        RewardTransactionDTO result = transactionDroolsDTO2RewardTransactionMapper.apply(trx);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("REJECTED", result.getStatus());
    }

    @Test
    void testWithRejectionReasons() {
        // Given
        TransactionDroolsDTO trx = TransactionDroolsDtoFaker.mockInstance(0);
        trx.setRewards(Map.of("INITIATIVE", new Reward(BigDecimal.TEN, BigDecimal.ONE, true)));
        trx.setRejectionReasons(List.of("REJECTION"));

        // When
        RewardTransactionDTO result = transactionDroolsDTO2RewardTransactionMapper.apply(trx);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("REJECTED", result.getStatus());
    }
}