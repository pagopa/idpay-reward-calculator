package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.TransactionProcessed;

import java.util.function.BiFunction;

public class TransactionProcessed2SyncTrxResponseDTOMapper implements BiFunction<BaseTransactionProcessed,String, SynchronousTransactionResponseDTO> {
    @Override
    public SynchronousTransactionResponseDTO apply(BaseTransactionProcessed baseTransactionProcessed, String initiativeId) {
        TransactionProcessed trx = (TransactionProcessed) baseTransactionProcessed;
        SynchronousTransactionResponseDTO out = new SynchronousTransactionResponseDTO();

        out.setTransactionId(trx.getId());
        out.setInitiativeId(initiativeId);
        out.setUserId(trx.getUserId());
        out.setStatus(trx.getStatus());
        out.setReward(trx.getRewards().get(initiativeId));
        return out;
    }
}
