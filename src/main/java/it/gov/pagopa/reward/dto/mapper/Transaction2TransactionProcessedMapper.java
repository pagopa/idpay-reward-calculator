package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

@Service
public class Transaction2TransactionProcessedMapper implements Function<RewardTransactionDTO, TransactionProcessed> {

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    public static final ZoneId ZONEID = ZoneId.of("Europe/Rome");

    @Override
    public TransactionProcessed apply(RewardTransactionDTO trx) {

        TransactionProcessed trxProcessed = null;

        if (trx != null) {
            trxProcessed = new TransactionProcessed();
            trxProcessed.setId(computeTrxId(trx));
            trxProcessed.setIdTrxAcquirer(trx.getIdTrxAcquirer());
            trxProcessed.setAcquirerCode(trx.getAcquirerCode());
            trxProcessed.setTrxDate(trx.getTrxDate().atZoneSameInstant(ZONEID).toLocalDateTime());
            trxProcessed.setOperationType(trx.getOperationType());
            trxProcessed.setAcquirerId(trx.getAcquirerId());
            trxProcessed.setUserId(trx.getUserId());
            trxProcessed.setCorrelationId(trx.getCorrelationId());
            trxProcessed.setAmount(trx.getAmount());
            trxProcessed.setRewards(trx.getRewards());
            trxProcessed.setEffectiveAmount(trx.getEffectiveAmount());
            trxProcessed.setTrxChargeDate(trx.getTrxChargeDate() != null ? trx.getTrxChargeDate().atZoneSameInstant(ZONEID).toLocalDateTime() : null);
            trxProcessed.setOperationTypeTranscoded(trx.getOperationTypeTranscoded());
        }
        return trxProcessed;
    }

    public String computeTrxId(TransactionDTO trx) {
        return trx.getIdTrxAcquirer()
                .concat(trx.getAcquirerCode())
                .concat(trx.getTrxDate().atZoneSameInstant(ZONEID).toLocalDateTime().format(DATETIME_FORMATTER))
                .concat(trx.getOperationType())
                .concat(trx.getAcquirerId());
    }
}