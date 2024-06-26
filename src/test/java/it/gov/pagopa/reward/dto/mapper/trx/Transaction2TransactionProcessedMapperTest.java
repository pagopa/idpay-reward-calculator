package it.gov.pagopa.reward.dto.mapper.trx;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
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
        LocalDateTime dateTime = LocalDateTime.of(2000, 1, 1, 23, 59, 57);
        OffsetDateTime trxDate = OffsetDateTime.of(dateTime, CommonConstants.ZONEID.getRules().getOffset(dateTime));
        Map<String, Reward> rewards = Map.of(
                "REWARDS0",
                new Reward("REWARDS0", "ORGANIZATION0", 100_00L)
        );

        RewardTransactionDTO trx = transaction2RewardTransactionMapper.apply(TransactionDTOFaker.mockInstance(0));
        trx.setTrxDate(trxDate);
        trx.setTrxChargeDate(trxDate);
        trx.setAmountCents(trx.getAmount().longValue());
        trx.setAmount(CommonUtilities.centsToEuro(trx.getAmountCents()));
        trx.setEffectiveAmountCents(CommonUtilities.euroToCents(trx.getAmount()));
        trx.setOperationTypeTranscoded(OperationType.CHARGE);
        trx.setRewards(rewards);
        trx.setId(TransactionDTO.computeTrxId(trx));
        trx.setRefundInfo(new RefundInfo());
        trx.setInitiatives(List.of("REWARDS0"));

        String id = "IDTRXACQUIRER0ACQUIRERCODE020000101T23595700ACQUIRERID0";

        // When
        TransactionProcessed result = transaction2TransactionProcessedMapper.apply(trx);

        //Then
        Assertions.assertNotNull(result);

        assertCommonFieldValues(trx, result);

        Assertions.assertEquals(id, result.getId());

        TestUtils.checkNotNullFields(result, "elaborationDateTime");
    }

    private void assertCommonFieldValues(RewardTransactionDTO trx, TransactionProcessed result) {
        LocalDateTime trxDate = trx.getTrxDate().atZoneSameInstant(CommonConstants.ZONEID).toLocalDateTime();
        final LocalDateTime trxChargeDate = trx.getTrxChargeDate().atZoneSameInstant(CommonConstants.ZONEID).toLocalDateTime();

        Assertions.assertSame(trx.getIdTrxAcquirer(), result.getIdTrxAcquirer());
        Assertions.assertSame(trx.getAcquirerCode(), result.getAcquirerCode());
        Assertions.assertEquals(trxDate, result.getTrxDate());
        Assertions.assertSame(trx.getOperationType(), result.getOperationType());
        Assertions.assertSame(trx.getCorrelationId(), result.getCorrelationId());
        Assertions.assertSame(trx.getAmount(), result.getAmount());
        Assertions.assertSame(trx.getAcquirerId(), result.getAcquirerId());
        Assertions.assertEquals(trx.getRewards(), result.getRewards());
        Assertions.assertEquals(trx.getUserId(), result.getUserId());
        Assertions.assertEquals(trx.getEffectiveAmountCents(), result.getEffectiveAmountCents());
        Assertions.assertEquals(trxChargeDate, result.getTrxChargeDate());
        Assertions.assertEquals(trx.getOperationTypeTranscoded(), result.getOperationTypeTranscoded());
        Assertions.assertEquals(trx.getStatus(), result.getStatus());
        Assertions.assertEquals(trx.getRejectionReasons(), result.getRejectionReasons());
        Assertions.assertEquals(trx.getInitiativeRejectionReasons(), result.getInitiativeRejectionReasons());
        Assertions.assertSame(trx.getRefundInfo(), result.getRefundInfo());
        Assertions.assertEquals(trx.getChannel(), result.getChannel());
        Assertions.assertSame(trx.getRuleEngineTopicPartition(), result.getRuleEngineTopicPartition());
        Assertions.assertSame(trx.getRuleEngineTopicOffset(), result.getRuleEngineTopicOffset());
    }


}