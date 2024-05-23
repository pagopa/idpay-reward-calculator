package it.gov.pagopa.reward.dto.trx;

import it.gov.pagopa.reward.enums.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LastTrxInfoDTO {
    private String trxId;
    private OperationType operationTypeTranscoded;
    private Map<String,Long> accruedReward;
    private LocalDateTime elaborationDateTime;
}
