package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.utils.RewardConstants;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.INVALID_COUNTER_VERSION;

public class InvalidCounterVersionException extends ServiceException {
    public InvalidCounterVersionException() {
        this(RewardConstants.ExceptionMessage.INVALID_COUNTER_VERSION);
    }

    public InvalidCounterVersionException(String message) {
        super(INVALID_COUNTER_VERSION, message);
    }

}