package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.synchronous.TransactionPreviewResponse;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Service;

@Service
public class RewardTransaction2PreviewResponseMapper implements TriFunction<String, String, RewardTransactionDTO, TransactionPreviewResponse> {
    @Override
    public TransactionPreviewResponse apply(String trxId, String initiativeId, RewardTransactionDTO rewardTransactionDTO) {
        TransactionPreviewResponse out = new TransactionPreviewResponse();

        out.setTransactionId(trxId);
        out.setInitiativeId(initiativeId);
        out.setUserId(rewardTransactionDTO.getUserId());
        out.setStatus(rewardTransactionDTO.getStatus());
        if(rewardTransactionDTO.getStatus().equals(RewardConstants.REWARD_STATE_REWARDED)) {
            out.setReward(rewardTransactionDTO.getRewards().get(initiativeId));
        } else {
            out.setRejectionReasons(rewardTransactionDTO.getRejectionReasons());
        }
        return out;
    }
}
