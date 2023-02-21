package it.gov.pagopa.reward.dto.trx;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.utils.RewardConstants;
import it.gov.pagopa.reward.utils.json.BigDecimalScale2Deserializer;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"idTrxAcquirer", "acquirerCode", "trxDate", "operationType", "acquirerId"}, callSuper = false)
@FieldNameConstants
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

    /** if {@link #amountCents} is null, this field will contain cents, otherwise it will contain euro */
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

    //region calculated fields
    private String id;
    private OperationType operationTypeTranscoded;
    @Builder.Default
    private List<String> rejectionReasons = new ArrayList<>();
    private Long amountCents;
    private BigDecimal effectiveAmount;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxChargeDate;
    private RefundInfo refundInfo;
    //endregion

    //region fields added by splitter
    private String userId;
    private String brandLogo;
    private String brand;
    private String maskedPan;
    //endregion


    public String getId() {
        if(this.id == null){
            this.id = computeTrxId(this);
        }
        return id;
    }

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    public static String computeTrxId(TransactionDTO trx) {
        return trx.getIdTrxAcquirer()
                .concat(trx.getAcquirerCode())
                .concat(trx.getTrxDate().atZoneSameInstant(RewardConstants.ZONEID).toLocalDateTime().format(DATETIME_FORMATTER))
                .concat(trx.getOperationType())
                .concat(trx.getAcquirerId());
    }
}
