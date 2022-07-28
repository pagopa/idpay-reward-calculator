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

    public Reward(BigDecimal reward){
        this.providedReward=reward;
        this.accruedReward=reward;
        this.capped=false;
    }

    public Reward(BigDecimal providedReward, BigDecimal accruedReward){
        this.providedReward=providedReward;
        this.accruedReward=accruedReward;
        this.capped=providedReward.compareTo(accruedReward)!=0;
    }
}
