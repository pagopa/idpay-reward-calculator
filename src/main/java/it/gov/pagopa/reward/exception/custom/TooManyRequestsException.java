package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.TOO_MANY_REQUESTS;

public class TooManyRequestsException extends ServiceException {
    public TooManyRequestsException(String message) {
        this(TOO_MANY_REQUESTS, message, null);
    }

    public TooManyRequestsException(String code, String message, ServiceExceptionPayload payload) {
        super(code, message, payload);
    }

}
