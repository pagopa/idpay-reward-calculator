package it.gov.pagopa.reward.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.reward.utils.json.BigDecimalScale2Deserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"idTrxAcquirer", "acquirerCode", "trxDate", "operationType", "acquirerId"}, callSuper = false)
public class TransactionDTO {
    private String idTrxAcquirer;

    private String acquirerCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxDate;

    private String hpan;

    private String operationType;

    private String circuitType;

    private String idTrxIssuer;

    private String correlationId;

    @JsonDeserialize(using = BigDecimalScale2Deserializer.class)
    private BigDecimal amount;

    private String amountCurrency;

    private String mcc;

    private String acquirerId;

    private String merchantId;

    private String terminalId;

    private String bin;

    private String senderCode;

    private String fiscalCode;

    private String vat;

    private String posType;

    private String par;


}
