package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewResponse;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RewardTransaction2PreviewResponseMapperTest {
    String trxId = "TRXID";
    String initiativeId = "INITIATIVEID";
    String userId = "USERID";
    @Test
    void applyRewardedTest(){
        // Given
        RewardTransaction2PreviewResponseMapper responseMapper = new RewardTransaction2PreviewResponseMapper();
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
                .status(RewardConstants.REWARD_STATE_REWARDED)
                .rewards(rewards)
                .elaborationDateTime(LocalDateTime.now())
                .build();

        // When
        TransactionPreviewResponse result = responseMapper.apply(trxId, initiativeId, trxDto);

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
        RewardTransaction2PreviewResponseMapper responseMapper = new RewardTransaction2PreviewResponseMapper();
        List<String> rejectionReasons = List.of(RewardConstants.TRX_REJECTION_REASON_INVALID_AMOUNT);
        RewardTransactionDTO trxDto = RewardTransactionDTO.builder()
                .userId(userId)
                .status(RewardConstants.REWARD_STATE_REJECTED)
                .rejectionReasons(rejectionReasons)
                .elaborationDateTime(LocalDateTime.now())
                .build();

        // When
        TransactionPreviewResponse result = responseMapper.apply(trxId, initiativeId, trxDto);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result, "reward");
        Assertions.assertEquals(RewardConstants.REWARD_STATE_REJECTED, result.getStatus());
        Assertions.assertEquals(rejectionReasons, result.getRejectionReasons());
        checkCommonAssertions(result);
    }

    void checkCommonAssertions(TransactionPreviewResponse result){
        Assertions.assertEquals(trxId, result.getTransactionId());
        Assertions.assertEquals(initiativeId, result.getInitiativeId());
        Assertions.assertEquals(userId, result.getUserId());
    }
}