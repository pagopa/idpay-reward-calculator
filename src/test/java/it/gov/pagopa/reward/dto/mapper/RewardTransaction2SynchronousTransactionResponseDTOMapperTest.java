package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RewardTransaction2SynchronousTransactionResponseDTOMapperTest {
    private final String trxId = "TRXID";
    private final String initiativeId = "INITIATIVEID";
    private final String userId = "USERID";
    private final String channel = "CHANNEL";
    private final OperationType operationType = OperationType.CHARGE;
    private final long amountCents = 100L;
    private final BigDecimal amount = BigDecimal.ONE;
    private final BigDecimal effectiveAmount = BigDecimal.ONE;

    @Test
    void applyRewardedTest(){
        // Given
        RewardTransaction2SynchronousTransactionResponseDTOMapper responseMapper = new RewardTransaction2SynchronousTransactionResponseDTOMapper();
        Reward reward = Reward.builder()
                .initiativeId(initiativeId)
                .organizationId("ORGANIZATIONID")
                .providedReward(BigDecimal.ONE)
                .accruedReward(BigDecimal.TEN)
                .build();
        Map<String, Reward> rewards = new HashMap<>();
        rewards.put(initiativeId, reward);
        RewardTransactionDTO trxDto = RewardTransactionDTO.builder()
                .userId(userId)
                .channel(channel)
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .operationTypeTranscoded(operationType)
                .amountCents(amountCents)
                .amount(amount)
                .effectiveAmount(effectiveAmount)
                .rewards(rewards)
                .elaborationDateTime(LocalDateTime.now())
                .build();

        // When
        SynchronousTransactionResponseDTO result = responseMapper.apply(trxId, initiativeId, trxDto);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "rejectionReasons");
        Assertions.assertEquals(RewardConstants.REWARD_STATE_REWARDED, result.getStatus());
        Assertions.assertEquals(reward, result.getReward());
        checkCommonAssertions(result);

    }
    @Test
    void applyRejectedTest(){
        // Given
        RewardTransaction2SynchronousTransactionResponseDTOMapper responseMapper = new RewardTransaction2SynchronousTransactionResponseDTOMapper();
        List<String> rejectionReasons = List.of(RewardConstants.TRX_REJECTION_REASON_INVALID_AMOUNT);
        RewardTransactionDTO trxDto = RewardTransactionDTO.builder()
                .userId(userId)
                .channel(channel)
                .operationTypeTranscoded(operationType)
                .amountCents(amountCents)
                .amount(effectiveAmount)
                .effectiveAmount(effectiveAmount)
                .status(RewardConstants.REWARD_STATE_REJECTED)
                .rejectionReasons(rejectionReasons)
                .elaborationDateTime(LocalDateTime.now())
                .build();

        // When
        SynchronousTransactionResponseDTO result = responseMapper.apply(trxId, initiativeId, trxDto);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.getReward());
        TestUtils.checkNotNullFields(result, "reward");
        Assertions.assertEquals(RewardConstants.REWARD_STATE_REJECTED, result.getStatus());
        Assertions.assertEquals(rejectionReasons, result.getRejectionReasons());
        checkCommonAssertions(result);
    }

    void checkCommonAssertions(SynchronousTransactionResponseDTO result){
        Assertions.assertEquals(trxId, result.getTransactionId());
        Assertions.assertEquals(channel, result.getChannel());
        Assertions.assertEquals(initiativeId, result.getInitiativeId());
        Assertions.assertEquals(userId, result.getUserId());
        Assertions.assertEquals(operationType, result.getOperationType());
        Assertions.assertEquals(amountCents, result.getAmountCents());
        Assertions.assertEquals(amount, result.getAmount());
        Assertions.assertEquals(effectiveAmount, result.getEffectiveAmount());
    }
}