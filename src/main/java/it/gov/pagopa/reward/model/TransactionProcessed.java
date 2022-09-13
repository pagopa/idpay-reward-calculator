package it.gov.pagopa.reward.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.utils.json.BigDecimalScale2Deserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "transactions_processed")
public class TransactionProcessed {
    private String idTrxAcquirer;

    private String acquirerCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxDate;

    private String operationType;

    private String acquirerId;

    private String userId;
    private String correlationId;
    @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
    private BigDecimal amount;

    private Map<String, Reward> rewards;

    private BigDecimal effectiveAmount;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxChargeDate;
    private OperationType operationTypeTranscoded;

    private OffsetDateTime timestamp; // TODO to fill
}

