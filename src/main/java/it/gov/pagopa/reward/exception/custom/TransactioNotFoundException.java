package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.TRANSACTION_NOT_FOUND;

public class TransactioNotFoundException extends ServiceException {

    public TransactioNotFoundException(String message) {
        this(TRANSACTION_NOT_FOUND, message,null);
    }

    public TransactioNotFoundException(String code, String message, ServiceExceptionPayload payload) {
        super(code, message, payload);
    }

}