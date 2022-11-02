package it.gov.pagopa.reward.dto.trx;

import it.gov.pagopa.reward.model.counters.RewardCounters;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reward {
    private String initiativeId;
    private String organizationId;

    /** The ruleEngine reward calculated */
    private BigDecimal providedReward;
    /** The effective reward after CAP and REFUND evaluation */
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

    /** True if is the trx has not more reward for the current initiative */
    private boolean isCompleteRefund;

    /** Counters */
    private RewardCounters counters;

    public Reward(String initiativeId, String organizationId, BigDecimal reward){
        this(initiativeId, organizationId, reward, reward, false);
    }

    public Reward(String initiativeId, String organizationId, BigDecimal providedReward, BigDecimal accruedReward){
        this(initiativeId, organizationId, providedReward, accruedReward, providedReward.compareTo(accruedReward)!=0);
    }

    public Reward(String initiativeId, String organizationId, BigDecimal providedReward, BigDecimal accruedReward, boolean capped){
        this.initiativeId=initiativeId;
        this.organizationId=organizationId;
        this.providedReward=providedReward;
        this.accruedReward=accruedReward;
        this.capped=capped;
    }
}
