package it.gov.pagopa.reward.dto.mapper.trx.recover;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
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

//region enriched data
        rewardedTrx.setTrxChargeDate(trxStored.getTrxChargeDate().atZone(CommonConstants.ZONEID).toOffsetDateTime());
        rewardedTrx.setAmountCents(trxStored.getAmountCents());
        rewardedTrx.setAmount(trxStored.getAmount());
        rewardedTrx.setEffectiveAmountCents(trxStored.getEffectiveAmountCents());
        rewardedTrx.setOperationTypeTranscoded(trxStored.getOperationTypeTranscoded());
//endregion

//region processing result data
        rewardedTrx.setRewards(trxStored.getRewards());
        rewardedTrx.setRejectionReasons(trxStored.getRejectionReasons());
        rewardedTrx.setInitiativeRejectionReasons(trxStored.getInitiativeRejectionReasons());
        rewardedTrx.setRefundInfo(trxStored.getRefundInfo());
        rewardedTrx.setStatus(trxStored.getStatus());
        rewardedTrx.setElaborationDateTime(trxStored.getElaborationDateTime());
        rewardedTrx.setInitiatives(trxStored.getInitiatives());
//endregion

        return rewardedTrx;
    }
}
