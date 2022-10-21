package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

class Transaction2TransactionProcessedMapperTest {

    private final Transaction2RewardTransactionMapper transaction2RewardTransactionMapper = new Transaction2RewardTransactionMapper();

    private final Transaction2TransactionProcessedMapper transaction2TransactionProcessedMapper = new Transaction2TransactionProcessedMapper();

    @Test
    void mapWithNullTransactionDTO() {
        // When
        TransactionProcessed result = transaction2TransactionProcessedMapper.apply(null);

        // Then
        Assertions.assertNull(result);
    }

    @Test
    void mapWithNotNullTransactionDTO(){
        // Given
        OffsetDateTime trxDate = OffsetDateTime.of(LocalDateTime.of(2000,1,1,23,59,57), RewardConstants.ZONEID.getRules().getOffset(LocalDateTime.now()));
        Map<String, Reward> rewards = Map.of(
                "REWARDS0",
                new Reward(BigDecimal.valueOf(100))
        );

        RewardTransactionDTO trx = transaction2RewardTransactionMapper.apply(TransactionDTOFaker.mockInstance(0));
        trx.setTrxDate(trxDate);
        trx.setTrxChargeDate(trxDate);
        trx.setEffectiveAmount(trx.getAmount());
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        trx.setRewards(rewards);
        trx.setId(TransactionDTO.computeTrxId(trx));

        String id = "IDTRXACQUIRER0ACQUIRERCODE020000101T22595700ACQUIRERID0";

        // When
        TransactionProcessed result = transaction2TransactionProcessedMapper.apply(trx);

        //Then
        Assertions.assertNotNull(result);

        assertCommonFieldValues(trx, result);

        Assertions.assertEquals(id, result.getId());

        TestUtils.checkNotNullFields(result, "timestamp");
    }

    private void assertCommonFieldValues(RewardTransactionDTO trx, TransactionProcessed result) {
        LocalDateTime trxDate = trx.getTrxDate().atZoneSameInstant(RewardConstants.ZONEID).toLocalDateTime();
        final LocalDateTime trxChargeDate = trx.getTrxChargeDate().atZoneSameInstant(RewardConstants.ZONEID).toLocalDateTime();

        Assertions.assertSame(trx.getIdTrxAcquirer(), result.getIdTrxAcquirer());
        Assertions.assertSame(trx.getAcquirerCode(), result.getAcquirerCode());
        Assertions.assertEquals(trxDate, result.getTrxDate());
        Assertions.assertSame(trx.getOperationType(), result.getOperationType());
        Assertions.assertSame(trx.getCorrelationId(), result.getCorrelationId());
        Assertions.assertSame(trx.getAmount(), result.getAmount());
        Assertions.assertSame(trx.getAcquirerId(), result.getAcquirerId());
        Assertions.assertEquals(trx.getRewards(), result.getRewards());
        Assertions.assertEquals(trx.getUserId(), result.getUserId());
        Assertions.assertEquals(trx.getEffectiveAmount(), result.getEffectiveAmount());
        Assertions.assertEquals(trxChargeDate, result.getTrxChargeDate());
        Assertions.assertEquals(trx.getOperationTypeTranscoded(), result.getOperationTypeTranscoded());
    }


}