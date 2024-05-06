package it.gov.pagopa.reward.dto.mapper.trx.sync;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;
import org.springframework.stereotype.Service;

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
        if(initiativeId!=null){
            out.setReward(Optional.ofNullable(trx.getRewards()).map(r->r.get(initiativeId)).orElse(null));
        }
        return out;
    }
}
