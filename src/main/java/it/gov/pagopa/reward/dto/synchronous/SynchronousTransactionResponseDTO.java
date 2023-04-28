package it.gov.pagopa.reward.dto.synchronous;

import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SynchronousTransactionResponseDTO {
    private String transactionId;
    private String channel;
    private String initiativeId;
    private String userId;
    private OperationType operationType;
    private Long amountCents;
    private BigDecimal amount;
    private BigDecimal effectiveAmount;
    private String status;
    private Reward reward;
    private List<String> rejectionReasons;
}
