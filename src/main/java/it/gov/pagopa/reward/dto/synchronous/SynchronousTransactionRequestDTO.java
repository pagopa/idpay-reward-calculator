package it.gov.pagopa.reward.dto.synchronous;

import it.gov.pagopa.reward.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SynchronousTransactionRequestDTO {
    private String transactionId;
    private String userId;
    private String merchantId;
    private String senderCode;
    private String merchantFiscalCode;
    private String vat;
    private String idTrxIssuer;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxDate;
    private Long amountCents;
    private String amountCurrency;
    private String mcc;
    private String acquirerCode;
    private String acquirerId;
    private String idTrxAcquirer;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private String correlationId;
    private OffsetDateTime trxChargeDate;
    private OperationType operationType;
    private String hpan;
}
