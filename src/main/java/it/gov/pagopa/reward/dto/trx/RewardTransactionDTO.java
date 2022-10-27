package it.gov.pagopa.reward.dto.trx;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class RewardTransactionDTO extends TransactionDTO {

    private String status;

    @Builder.Default
    private Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();

    private Map<String, Reward> rewards;
}
