package it.gov.pagopa.reward.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.utils.json.BigDecimalScale2Deserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A transaction that has been elaborated */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "transactions_processed")
public class TransactionProcessed implements BaseTransactionProcessed {
    @Id
    private String id;

    private String idTrxAcquirer;

    private String acquirerCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime trxDate;

    private String operationType;

    private String acquirerId;

    private String userId;

    private String correlationId;

    @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
    private BigDecimal amount;

    private Map<String, Reward> rewards;
    private String status;

    @Builder.Default
    private List<String> rejectionReasons = new ArrayList<>();
    private Map<String, List<String>> initiativeRejectionReasons;

    private BigDecimal effectiveAmount;
    private Long amountCents;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime trxChargeDate;
    private OperationType operationTypeTranscoded;

    private LocalDateTime elaborationDateTime;
}

