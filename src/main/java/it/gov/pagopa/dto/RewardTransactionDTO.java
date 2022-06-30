package it.gov.pagopa.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = {"idTrxAcquirer", "acquirerCode", "trxDate", "operationType", "acquirerId"}, callSuper = false)
public class RewardTransactionDTO {

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

    BigDecimal reward;

    String senderCode;

    String fiscalCode;

    String vat;

    String posType;

    String par;

}
