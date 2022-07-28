package it.gov.pagopa.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reward {
    /** The ruleEngine reward calculated */
    private BigDecimal providedReward;
    /** The effective reward after CAP evaluation */
    private BigDecimal accruedReward;
    /** True, if the reward has been capped */
    private boolean capped;
}
