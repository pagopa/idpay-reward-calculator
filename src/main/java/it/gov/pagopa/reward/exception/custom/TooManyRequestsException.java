package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.TOO_MANY_REQUESTS;

public class TooManyRequestsException extends ServiceException {

    public TooManyRequestsException(String message, SynchronousTransactionResponseDTO response) {
        this(TOO_MANY_REQUESTS, message,response);
    }
    public TooManyRequestsException(String code, String message, SynchronousTransactionResponseDTO response) {
        super(code, message, response);
    }

    public TooManyRequestsException(String code, String message, boolean printStackTrace, Throwable ex, SynchronousTransactionResponseDTO response) {
        super(code, message, printStackTrace, ex, response);
    }
}
