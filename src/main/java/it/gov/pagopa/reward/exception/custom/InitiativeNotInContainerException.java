package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.INITIATIVE_NOT_READY;

public class InitiativeNotInContainerException  extends ServiceException {

    public InitiativeNotInContainerException(String message, ServiceExceptionPayload payload) {
        this(INITIATIVE_NOT_READY, message,payload);
    }
    public InitiativeNotInContainerException(String code, String message, ServiceExceptionPayload payload) {
        super(code, message, payload);
    }

}
