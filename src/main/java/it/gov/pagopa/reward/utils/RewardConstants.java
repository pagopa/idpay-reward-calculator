package it.gov.pagopa.reward.utils;

public final class RewardConstants {
    private RewardConstants(){}

    //region rules' order
    public enum InitiativeTrxConditionOrder {
        MCCFILTER,
        THRESHOLD,
        REWARDGROUP,
        DAYOFWEEK,
        REWARDLIMITS,
        TRXCOUNT;

        public String getRejectionReason(){
            return "TRX_RULE_%s_FAIL".formatted(name());
        }
    }
    //endregion
}
