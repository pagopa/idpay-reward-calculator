package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.function.Function;

@Service
public class TransactionDroolsDTO2RewardTransactionMapper implements Function<TransactionDroolsDTO, RewardTransactionDTO> {
    @Override
    public RewardTransactionDTO apply(TransactionDroolsDTO rewardTrx) {
        RewardTransactionDTO trxDto = null;

        if (rewardTrx != null) {
            trxDto = new RewardTransactionDTO();
            Transaction2RewardTransactionMapper.copyFields(rewardTrx, trxDto);
            trxDto.setInitiativeRejectionReasons(rewardTrx.getInitiativeRejectionReasons());
            trxDto.setInitiatives(rewardTrx.getInitiatives());
            trxDto.setRewards(rewardTrx.getRewards());

            trxDto.setStatus(
                    CollectionUtils.isEmpty(rewardTrx.getRejectionReasons()) &&
                            rewardTrx.getRewards().values().stream().anyMatch(r->r.getAccruedReward().compareTo(BigDecimal.ZERO)!=0)
                            ? RewardConstants.REWARD_STATE_REWARDED
                            : RewardConstants.REWARD_STATE_REJECTED
            );
        }

        return trxDto;
    }
}
