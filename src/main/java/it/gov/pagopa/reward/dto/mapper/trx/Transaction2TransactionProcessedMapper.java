package it.gov.pagopa.reward.dto.mapper.trx;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
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
            trxProcessed.setTrxDate(trx.getTrxDate().atZoneSameInstant(CommonConstants.ZONEID).toLocalDateTime());
            trxProcessed.setOperationType(trx.getOperationType());
            trxProcessed.setAcquirerId(trx.getAcquirerId());
            trxProcessed.setUserId(trx.getUserId());
            trxProcessed.setCorrelationId(trx.getCorrelationId());
            trxProcessed.setAmount(trx.getAmount());
            trxProcessed.setAmountCents(trx.getAmountCents());
            trxProcessed.setEffectiveAmount(trx.getEffectiveAmount());
            trxProcessed.setRewards(trx.getRewards());
            trxProcessed.setTrxChargeDate(trx.getTrxChargeDate() != null ? trx.getTrxChargeDate().atZoneSameInstant(CommonConstants.ZONEID).toLocalDateTime() : null);
            trxProcessed.setOperationTypeTranscoded(trx.getOperationTypeTranscoded());
            trxProcessed.setStatus(trx.getStatus());
            trxProcessed.setRejectionReasons(trx.getRejectionReasons());
            trxProcessed.setRefundInfo(trx.getRefundInfo());
            trxProcessed.setInitiativeRejectionReasons(trx.getInitiativeRejectionReasons());
            trxProcessed.setChannel(trx.getChannel());
            trxProcessed.setRuleEngineTopicPartition(trx.getRuleEngineTopicPartition());
            trxProcessed.setRuleEngineTopicOffset(trx.getRuleEngineTopicOffset());
            trxProcessed.setInitiatives(trx.getInitiatives());
        }
        return trxProcessed;
    }
}