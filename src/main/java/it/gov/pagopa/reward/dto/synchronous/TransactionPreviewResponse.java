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
public class TransactionPreviewResponse {
    private String transactionId;
    private String initiativeId;
    private String userId;
    private String status; //TODO TVB constants
    private Reward reward;
    private List<String> rejectionReasons; //TODO TVB constants
}
