package it.gov.pagopa.reward.dto.synchronous;

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
    private String initiativeId;
    private String userId;
    private String status;
    private BigDecimal reward; //TODO
    private List<String> rejectionReasons;
}
