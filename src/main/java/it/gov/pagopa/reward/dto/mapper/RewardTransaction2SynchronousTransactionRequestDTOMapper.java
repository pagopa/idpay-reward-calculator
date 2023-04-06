package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RewardTransaction2SynchronousTransactionRequestDTOMapper implements TriFunction<String, String, RewardTransactionDTO, SynchronousTransactionResponseDTO> {
    @Override
    public SynchronousTransactionResponseDTO apply(String trxId, String initiativeId, RewardTransactionDTO rewardTransactionDTO) {
        SynchronousTransactionResponseDTO out = new SynchronousTransactionResponseDTO();

        out.setTransactionId(trxId);
        out.setInitiativeId(initiativeId);
        out.setUserId(rewardTransactionDTO.getUserId());
        out.setStatus(rewardTransactionDTO.getStatus());
        if(rewardTransactionDTO.getStatus().equals(RewardConstants.REWARD_STATE_REWARDED)) {
            out.setReward(rewardTransactionDTO.getRewards().get(initiativeId).getProvidedReward());
        } else {
            out.setReward(BigDecimal.ZERO);
            out.setRejectionReasons(rewardTransactionDTO.getRejectionReasons());
        }
        return out;
    }
}
