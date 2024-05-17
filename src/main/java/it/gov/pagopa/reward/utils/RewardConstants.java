package it.gov.pagopa.reward.utils;

import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class RewardConstants {
    private RewardConstants(){}

    //region transactions' channels
    public static final String TRX_CHANNEL_RTD = "RTD";
    public static final String TRX_CHANNEL_QRCODE = "QRCODE";
    public static final String TRX_CHANNEL_IDPAYCODE = "IDPAYCODE";
    public static final String TRX_CHANNEL_BARCODE = "BARCODE";
    //endregion

    //region rules' order and default rejection reasons
    /** The order of execution of the trx condition */
    public enum InitiativeTrxConditionOrder {
        REWARDLIMITS {
            /** Use the rejection reason as format where insert the {@link RewardLimitsDTO.RewardLimitFrequency#name()} */
            @Override
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
    public static final int INITIATIVE_TRX_CONSEQUENCE_TRX_COUNT_ORDER = INITIATIVE_TRX_CONSEQUENCE_REWARD_LIMITS_ORDER - 1;
    //endregion

    //region reward evaluation rejection reasons
    public static final String TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID = "DUPLICATE_CORRELATION_ID";
    public static final String TRX_REJECTION_REASON_NO_INITIATIVE = "NO_ACTIVE_INITIATIVES";
    public static final String TRX_REJECTION_REASON_INVALID_OPERATION_TYPE = "INVALID_OPERATION_TYPE";
    public static final String TRX_REJECTION_REASON_INVALID_AMOUNT = "INVALID_AMOUNT";
    public static final String TRX_REJECTION_REASON_INVALID_REFUND = "INVALID_REFUND";
    public static final String TRX_REJECTION_REASON_REFUND_NOT_MATCH = "REFUND_NOT_MATCH";
    public static final String TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND = "INITIATIVE_NOT_FOUND";
    public static final String TRX_REJECTION_REASON_BUDGET_EXHAUSTED = "BUDGET_EXHAUSTED";
    public static final String TRX_REJECTION_REASON_RULE_ENGINE_NOT_READY = "RULE_ENGINE_NOT_READY";
    //endregion

    //region reward status
    public static final String REWARD_STATE_REWARDED = "REWARDED";
    public static final String REWARD_STATE_REJECTED = "REJECTED";
    //endregion

    //region payment status
    public static final String PAYMENT_STATE_AUTHORIZED = "AUTHORIZED";
    public static final String PAYMENT_STATE_REWARDED = "REWARDED";
    public static final String PAYMENT_STATE_REJECTED = "REJECTED";
    //endregion

    //region dateFormatters
    public static final DateTimeFormatter dayDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter weekDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-W", Locale.ITALY);
    public static final DateTimeFormatter monthDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    public static final DateTimeFormatter yearDateFormatter = DateTimeFormatter.ofPattern("yyyy");
    //endregion

    public static final class ExceptionCode {
        private ExceptionCode() {}

        public static final String INITIATIVE_NOT_ACTIVE_FOR_USER = "REWARD_CALCULATOR_INITIATIVE_NOT_ACTIVE_FOR_USER";
        public static final String INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT = "REWARD_CALCULATOR_INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT";
        public static final String INITIATIVE_NOT_READY = "REWARD_CALCULATOR_INITIATIVE_NOT_READY";
        public static final String INVALID_COUNTER_VERSION = "INVALID_COUNTER_VERSION";
        public static final String PENDING_COUNTER = "PENDING_COUNTER";
        public static final String TOO_MANY_REQUESTS = "REWARD_CALCULATOR_TOO_MANY_REQUESTS";
        public static final String GENERIC_ERROR = "REWARD_CALCULATOR_GENERIC_ERROR";
        public static final String INVALID_REQUEST= "REWARD_CALCULATOR_INVALID_REQUEST";
        public static final String TRANSACTION_ALREADY_AUTHORIZED = "REWARD_CALCULATOR_TRX_ALREADY_AUTHORIZED";
    }

    public static final class ExceptionMessage {
        private ExceptionMessage() {}

        public static final String INITIATIVE_NOT_ACTIVE_FOR_USER_MSG = "The initiative with id [%s] is not active for the current user";
        public static final String INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT_MSG = "The initiative with id [%s] is not found or not discount";
        public static final String INITIATIVE_NOT_READY_MSG = "The initiative with id [%s] is not ready";
        public static final String INVALID_COUNTER_VERSION = "Invalid counter version provided";
        public static final String PENDING_COUNTER = "The counter is actually locked";
        public static final String TRANSACTION_ALREADY_AUTHORIZED_MSG = "The transaction with id [%s] is already authorized";
    }
}
