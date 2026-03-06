package it.gov.pagopa.reward.dto.synchronous;

import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SynchronousTransactionResponseDTO implements ServiceExceptionPayload {
    private String transactionId;
    private String channel;
    private String initiativeId;
    private String userId;
    private OperationType operationType;
    private Long amountCents;
    private BigDecimal amount;
    private Long effectiveAmountCents;
    private String status;
    private Reward reward;
    private Map<String, Reward> rewards;
    private Long trxNumber;
    private Long totalAmountCents;
    private Long totalRewardCents;
    private List<String> rejectionReasons;
}
