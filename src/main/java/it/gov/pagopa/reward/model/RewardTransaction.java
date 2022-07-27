package it.gov.pagopa.reward.model;

import it.gov.pagopa.reward.dto.Reward;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = {"idTrxAcquirer","acquirerCode", "trxDate", "operationType", "acquirerId"}, callSuper = false)
public class RewardTransaction {
    String idTrxAcquirer;

    String acquirerCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    OffsetDateTime trxDate;

    String hpan;

    String operationType;

    String circuitType;

    String idTrxIssuer;

    String correlationId;

    BigDecimal amount;

    String amountCurrency;

    String mcc;

    String acquirerId;

    String merchantId;

    String terminalId;

    String bin;

    String senderCode;

    String fiscalCode;

    String vat;

    String posType;

    String par;

    String status;

    List<String> rejectionReason;

    List<String> initiatives;

    Map<String, Reward> rewards;
}

