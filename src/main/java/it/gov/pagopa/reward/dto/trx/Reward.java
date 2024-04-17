package it.gov.pagopa.reward.dto.trx;

import it.gov.pagopa.reward.model.counters.RewardCounters;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@FieldNameConstants
public class Reward {
    private String initiativeId;
    private String organizationId;
    private String familyId;

    /** The ruleEngine reward calculated */
    private Long providedRewardCents;
    /** The effective reward after CAP and REFUND evaluation */
    private Long accruedRewardCents;
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

    /** True if it's a refunding reward */
    private boolean refund;
    /** True if it's a complete refunding reward */
    private boolean completeRefund;

    /** Counters */
    private RewardCounters counters;

    public Reward(String initiativeId, String organizationId, Long rewardCents){
        this(initiativeId, organizationId, rewardCents, false);
    }
    public Reward(String initiativeId, String organizationId, Long rewardCents, boolean refund){
        this(initiativeId, organizationId, rewardCents, rewardCents, false, refund);
    }

    public Reward(String initiativeId, String organizationId, Long providedRewardCents, Long accruedRewardCents){
        this(initiativeId, organizationId, providedRewardCents, accruedRewardCents, providedRewardCents.compareTo(accruedRewardCents)!=0, false);
    }

    public Reward(String initiativeId, String organizationId, Long providedRewardCents, Long accruedRewardCents, boolean capped, boolean refund){
        this.initiativeId=initiativeId;
        this.organizationId=organizationId;
        this.providedRewardCents=providedRewardCents;
        this.accruedRewardCents=accruedRewardCents;
        this.capped=capped;
        this.refund = refund;
    }
}
