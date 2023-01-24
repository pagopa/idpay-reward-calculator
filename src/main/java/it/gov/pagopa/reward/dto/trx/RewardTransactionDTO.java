package it.gov.pagopa.reward.dto.trx;

import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class RewardTransactionDTO extends TransactionDTO implements BaseTransactionProcessed {

    private String status;

    @Builder.Default
    private Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

    private Map<String, Reward> rewards;

    @Builder.Default
    private LocalDateTime elaborationDateTime = LocalDateTime.now();
}
