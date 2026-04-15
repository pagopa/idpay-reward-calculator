package it.gov.pagopa.reward.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.gov.pagopa.common.utils.json.BigDecimalScale2Deserializer;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.enums.OperationType;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class TransactionDroolsDTO {

    
    private List<String> rejectionReasons = new ArrayList<>();

    
    private Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

    private List<String> initiatives;

    
    private Map<String, Reward> rewards = new HashMap<>();
    private String idTrxAcquirer;

    private String acquirerCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxDate;

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
    @JsonAlias("_id")
    private String id;
    private OperationType operationTypeTranscoded;

    private Long amountCents;
    private Long effectiveAmountCents;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime trxChargeDate;
    private RefundInfo refundInfo;
    private String channel;

    private Integer ruleEngineTopicPartition;
    private Long ruleEngineTopicOffset;
    //endregion

    //region fields added by splitter
    private String userId;
    //endregion

    //region fields added by self-expense
    private String businessName;

    //voucher amount
    private Long voucherAmountCents;

    //region fields added by payment
    private String familyId;


}

