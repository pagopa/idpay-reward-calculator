package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class RewardTransaction2SynchronousTransactionResponseDTOMapper implements TriFunction<String, String, RewardTransactionDTO, SynchronousTransactionResponseDTO> {
    @Override
    public SynchronousTransactionResponseDTO apply(String trxId, String initiativeId, RewardTransactionDTO rewardTransactionDTO) {
        SynchronousTransactionResponseDTO out = new SynchronousTransactionResponseDTO();

        out.setTransactionId(trxId);
        out.setChannel(rewardTransactionDTO.getChannel());
        out.setInitiativeId(initiativeId);
        out.setUserId(rewardTransactionDTO.getUserId());
        out.setOperationType(rewardTransactionDTO.getOperationTypeTranscoded());
        out.setAmount(rewardTransactionDTO.getAmountCents());
        out.setEffectiveAmount(rewardTransactionDTO.getEffectiveAmount());
        out.setStatus(rewardTransactionDTO.getStatus());
        if(rewardTransactionDTO.getStatus().equals(RewardConstants.REWARD_STATE_REWARDED)) {
            out.setReward(rewardTransactionDTO.getRewards().get(initiativeId));
        } else {
            setRejectionReasons(initiativeId, rewardTransactionDTO, out);
        }
        return out;
    }

    private void setRejectionReasons(String initiativeId, RewardTransactionDTO rewardTransactionDTO, SynchronousTransactionResponseDTO out) {
        List<String> initiativeRejection = rewardTransactionDTO.getInitiativeRejectionReasons().get(initiativeId);
        if(initiativeRejection==null){
            out.setRejectionReasons(rewardTransactionDTO.getRejectionReasons());
        } else {
            out.setRejectionReasons(Stream.concat(initiativeRejection.stream(),
                    rewardTransactionDTO.getRejectionReasons().stream()).toList());
        }
    }
}
