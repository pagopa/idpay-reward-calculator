package it.gov.pagopa.reward.dto;

import it.gov.pagopa.reward.model.counters.RewardCounters;
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
    /** True, if the reward has been capped due to budget threshold */
    private boolean capped;
    /** True, if the reward has been capped due to daily threshold */
    private boolean dailyCapped;
    /** True, if the reward has been capped due to monthly threshold */
    private boolean monthlyCapped;
    /** True, if the reward has been capped due to yearly threshold */
    private boolean yearlyCapped;
    /** True, if the reward has been capped due to weekly threshold */
    private boolean weeklyCapped;
    /** Counters */
    private RewardCounters counters;

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

    public Reward(BigDecimal providedReward, BigDecimal accruedReward, boolean capped){
        this.providedReward=providedReward;
        this.accruedReward=accruedReward;
        this.capped=capped;
    }
}
