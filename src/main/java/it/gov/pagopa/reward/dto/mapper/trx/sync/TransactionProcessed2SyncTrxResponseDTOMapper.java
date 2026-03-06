package it.gov.pagopa.reward.dto.mapper.trx.sync;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@Service
public class TransactionProcessed2SyncTrxResponseDTOMapper implements BiFunction<BaseTransactionProcessed,String, SynchronousTransactionResponseDTO> {
    @Override
    public SynchronousTransactionResponseDTO apply(BaseTransactionProcessed baseTransactionProcessed, String initiativeId) {
        TransactionProcessed trx = (TransactionProcessed) baseTransactionProcessed;
        SynchronousTransactionResponseDTO out = new SynchronousTransactionResponseDTO();

        out.setTransactionId(trx.getId());
        out.setChannel(trx.getChannel());
        out.setInitiativeId(initiativeId);
        out.setUserId(trx.getUserId());
        out.setOperationType(baseTransactionProcessed.getOperationTypeTranscoded());
        out.setAmountCents(baseTransactionProcessed.getAmountCents());
        out.setAmount(baseTransactionProcessed.getAmount());
        out.setEffectiveAmountCents(baseTransactionProcessed.getEffectiveAmountCents());
        out.setStatus(trx.getStatus());
        out.setRewards(trx.getRewards() != null ? trx.getRewards() : Map.of());
        if(initiativeId!=null){out.setReward(Optional.ofNullable(out.getRewards()).map(r->r.get(initiativeId)).orElse(null));}
        Reward reward = out.getReward();
        if (reward != null && reward.getCounters() != null) {
            out.setTrxNumber(reward.getCounters().getTrxNumber());
            out.setTotalAmountCents(reward.getCounters().getTotalAmountCents());
            out.setTotalRewardCents(reward.getCounters().getTotalRewardCents());
        }
        if (out.getTrxNumber() == null) out.setTrxNumber(0L);
        if (out.getTotalAmountCents() == null) out.setTotalAmountCents(0L);
        if (out.getTotalRewardCents() == null) out.setTotalRewardCents(0L);
        return out;
    }
}
