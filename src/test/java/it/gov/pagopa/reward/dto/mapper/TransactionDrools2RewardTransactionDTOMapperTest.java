package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
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
        trx.setRefundInfo(new RefundInfo());

        trx.setRewards(Map.of("INITIATIVE1", new Reward("INITIATIVE1", "ORGANIZATION1", BigDecimal.ZERO, BigDecimal.ZERO, false)));
        trx.setRewards(Map.of("INITIATIVE2", new Reward("INITIATIVE2", "ORGANIZATION2", BigDecimal.TEN, BigDecimal.ZERO, true)));
        trx.setRewards(Map.of("INITIATIVE3", new Reward("INITIATIVE3", "ORGANIZATION3", BigDecimal.TEN, BigDecimal.ONE, true)));

        trx.setInitiatives(new ArrayList<>());

        // When
        RewardTransactionDTO result = transactionDroolsDTO2RewardTransactionMapper.apply(trx);

        // Then
        Assertions.assertNotNull(result);

        Transaction2RewardTransactionDTOMapperTest.assertCommonFieldsValues(trx, result);

        Assertions.assertEquals("REWARDED", result.getStatus());

        TestUtils.checkNotNullFields(result);
    }

    @Test
    void testWithNoRewards() {
        // Given
        TransactionDroolsDTO trx = TransactionDroolsDtoFaker.mockInstance(0);
        trx.setRewards(Map.of("INITIATIVE1", new Reward("INITIATIVE1", "ORGANIZATION1", BigDecimal.ZERO, BigDecimal.ZERO, false)));
        trx.setRewards(Map.of("INITIATIVE2", new Reward("INITIATIVE2", "ORGANIZATION2", BigDecimal.TEN, BigDecimal.ZERO, true)));

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
        trx.setRewards(Map.of("INITIATIVE", new Reward("INITIATIVE", "ORGANIZATION", BigDecimal.TEN, BigDecimal.ONE, true)));
        trx.setRejectionReasons(List.of("REJECTION"));

        // When
        RewardTransactionDTO result = transactionDroolsDTO2RewardTransactionMapper.apply(trx);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals("REJECTED", result.getStatus());
    }
}