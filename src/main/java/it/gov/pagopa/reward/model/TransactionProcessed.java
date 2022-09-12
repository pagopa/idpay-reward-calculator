package it.gov.pagopa.reward.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.utils.json.BigDecimalScale2Deserializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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
    @Id
    private String id;

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
}

