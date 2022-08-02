package it.gov.pagopa.reward.utils;

import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;

public final class RewardConstants {
    private RewardConstants(){}

    //region rules' order and default rejection reasons
    /** The order of execution of the trx condition */
    public enum InitiativeTrxConditionOrder {
        MCCFILTER,
        THRESHOLD,
        REWARDGROUPS_CONDITION,
        DAYOFWEEK,
        REWARDLIMITS {
            /** Use the rejection reason as format where insert the {@link RewardLimitsDTO.RewardLimitFrequency#name()} */
            public String getRejectionReason() {
                return "TRX_RULE_%s_%%s_FAIL".formatted(name());
            }

            @Override
            public int getOrder() {
                return INITIATIVE_TRX_CONSEQUENCE_ORDER - 1;
            }
        },
        TRXCOUNT;

        public String getRejectionReason(){
            return "TRX_RULE_%s_FAIL".formatted(name());
        }

        public int getOrder(){
            return ordinal();
        }
    }

    public static int INITIATIVE_TRX_CONSEQUENCE_ORDER = -1;
    public static int INITIATIVE_TRX_CONSEQUENCE_REWARD_LIMITS_ORDER = InitiativeTrxConditionOrder.REWARDLIMITS.getOrder() - 1;
    //endregion
}
