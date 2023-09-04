package it.gov.pagopa.reward.model;

import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.enums.OperationType;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;

@Document(collection = "transactions_processed")
public interface BaseTransactionProcessed {
    String getId();
    String getUserId();
    String getIdTrxAcquirer();
    String getAcquirerCode();
    String getAcquirerId();
    String getCorrelationId();
    String getOperationType();
    OperationType getOperationTypeTranscoded();
    Temporal getTrxDate();
    Temporal getTrxChargeDate();
    BigDecimal getAmount();
    Long getAmountCents();
    BigDecimal getEffectiveAmount();
    String getChannel();
    List<String> getRejectionReasons();
    Map<String, List<String>> getInitiativeRejectionReasons();

    Map<String, Reward> getRewards();
    List<String> getInitiatives();

    LocalDateTime getElaborationDateTime();
    void setElaborationDateTime(LocalDateTime elaborationDateTime);

    Integer getRuleEngineTopicPartition();
    Long getRuleEngineTopicOffset();
}
