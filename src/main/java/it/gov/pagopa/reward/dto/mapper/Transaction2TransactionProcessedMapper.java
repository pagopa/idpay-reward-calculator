package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class Transaction2TransactionProcessedMapper implements Function<RewardTransactionDTO, TransactionProcessed> {

    @Override
    public TransactionProcessed apply(RewardTransactionDTO trx) {

        TransactionProcessed trxProcessed = null;

        if (trx != null) {
            trxProcessed = new TransactionProcessed();
            trxProcessed.setId(trx.getId());
            trxProcessed.setIdTrxAcquirer(trx.getIdTrxAcquirer());
            trxProcessed.setAcquirerCode(trx.getAcquirerCode());
            trxProcessed.setTrxDate(trx.getTrxDate().atZoneSameInstant(RewardConstants.ZONEID).toLocalDateTime());
            trxProcessed.setOperationType(trx.getOperationType());
            trxProcessed.setAcquirerId(trx.getAcquirerId());
            trxProcessed.setUserId(trx.getUserId());
            trxProcessed.setCorrelationId(trx.getCorrelationId());
            trxProcessed.setAmount(trx.getAmount());
            trxProcessed.setRewards(trx.getRewards());
            trxProcessed.setEffectiveAmount(trx.getEffectiveAmount());
            trxProcessed.setAmountCents(trx.getAmountCents());
            trxProcessed.setTrxChargeDate(trx.getTrxChargeDate() != null ? trx.getTrxChargeDate().atZoneSameInstant(RewardConstants.ZONEID).toLocalDateTime() : null);
            trxProcessed.setOperationTypeTranscoded(trx.getOperationTypeTranscoded());
            trxProcessed.setStatus(trx.getStatus());
            trxProcessed.setRejectionReasons(trx.getRejectionReasons());
            trxProcessed.setInitiativeRejectionReasons(trx.getInitiativeRejectionReasons());
            trxProcessed.setChannel(trx.getChannel());
        }
        return trxProcessed;
    }
}