package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import org.springframework.stereotype.Service;

@Service
public class RecoveredTrx2RewardTransactionMapper {

    private final Transaction2RewardTransactionMapper transaction2RewardTransactionMapper;

    public RecoveredTrx2RewardTransactionMapper(Transaction2RewardTransactionMapper transaction2RewardTransactionMapper) {
        this.transaction2RewardTransactionMapper = transaction2RewardTransactionMapper;
    }

    public RewardTransactionDTO apply(TransactionDTO trx, TransactionProcessed trxStored) {
        RewardTransactionDTO rewardedTrx = transaction2RewardTransactionMapper.apply(trx);
        rewardedTrx.setRewards(trxStored.getRewards());
        rewardedTrx.setRejectionReasons(trxStored.getRejectionReasons());
        rewardedTrx.setInitiativeRejectionReasons(trxStored.getInitiativeRejectionReasons());
        rewardedTrx.setStatus(trxStored.getStatus());
        rewardedTrx.setElaborationDateTime(trxStored.getElaborationDateTime());

        return rewardedTrx;
    }
}
