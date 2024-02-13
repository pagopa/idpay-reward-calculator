package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.utils.RewardConstants;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.PENDING_COUNTER;

public class PendingCounterException extends ServiceException {
    public PendingCounterException() {
    this(RewardConstants.ExceptionMessage.PENDING_COUNTER);
}
    public PendingCounterException(String message) {
        super(PENDING_COUNTER, message);
    }

}