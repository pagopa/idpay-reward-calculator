package it.gov.pagopa.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"idTrxAcquirer", "acquirerCode", "trxDate"}, callSuper = false)
public class TransactionDTO {
    String idTrxAcquirer;

    String acquirerCode;

    String trxDate;

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


}
