package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.CONFLICT_ERROR;

public class TransactionAlreadyProcessedException extends ServiceException {

    public TransactionAlreadyProcessedException(ServiceExceptionPayload payload) {
        this(CONFLICT_ERROR,payload);
    }
    public TransactionAlreadyProcessedException(String message, ServiceExceptionPayload payload) {
        this(CONFLICT_ERROR, message,payload);
    }
    public TransactionAlreadyProcessedException(String code, String message, ServiceExceptionPayload payload) {
        super(code, message, payload);
    }

}