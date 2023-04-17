package it.gov.pagopa.reward.dto.synchronous;

import it.gov.pagopa.reward.dto.trx.Reward;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String status;
    private Reward reward;
    private List<String> rejectionReasons;
}
