package it.gov.pagopa.reward.utils;

import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;

public final class RewardConstants {
    private RewardConstants(){}

    //region initiative's build rejection reasons
    public static final String INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED = "BUDGET_EXHAUSTED";
    //endregion

    //region rules' order and default rejection reasons
    /** The order of execution of the trx condition */
    public enum InitiativeTrxConditionOrder {
        REWARDLIMITS {
            /** Use the rejection reason as format where insert the {@link RewardLimitsDTO.RewardLimitFrequency#name()} */
            public String getRejectionReason() {
                return "TRX_RULE_%s_%%s_FAIL".formatted(name());
            }
        },
        TRXCOUNT,
        MCCFILTER,
        THRESHOLD,
        REWARDGROUPS_CONDITION,
        DAYOFWEEK;

        public String getRejectionReason(){
            return "TRX_RULE_%s_FAIL".formatted(name());
        }

        public int getOrder(){
            return ordinal();
        }
    }

    public static final int INITIATIVE_TRX_CONSEQUENCE_ORDER = -1;
    public static final int INITIATIVE_TRX_CONSEQUENCE_REWARD_LIMITS_ORDER = INITIATIVE_TRX_CONSEQUENCE_ORDER - 1;
    //endregion

    //region reward evaluation rejection reasons
    public static final String TRX_REJECTION_REASON_NO_INITIATIVE = "NO_ACTIVE_INITIATIVES";
    public static final String TRX_REJECTION_REASON_INVALID_OPERATION_TYPE = "INVALID_OPERATION_TYPE";
    //endregion

    //region reward status
    public static final String REWARD_STATE_REWARDED = "REWARDED";
    public static final String REWARD_STATE_REJECTED = "REJECTED";
    //endregion
}
