package it.gov.pagopa.reward.utils;

public final class RewardConstants {
    private RewardConstants(){}

    //region rules' order
    /** The order of execution of the trx condition */
    public enum InitiativeTrxConditionOrder {
        MCCFILTER,
        THRESHOLD,
        REWARDGROUPS_CONDITION,
        DAYOFWEEK,
        REWARDLIMITS,
        TRXCOUNT;

        public String getRejectionReason(){
            return "TRX_RULE_%s_FAIL".formatted(name());
        }
    }
    //endregion
}
