package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.common.web.exception.ServiceWithPayloadException;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.INITIATIVE_NOT_READY;

@SuppressWarnings("squid:S110")
public class InitiativeNotInContainerException  extends ServiceWithPayloadException {

    public InitiativeNotInContainerException(String message, ServiceExceptionPayload payload) {
        this(INITIATIVE_NOT_READY, message,payload);
    }
    public InitiativeNotInContainerException(String code, String message, ServiceExceptionPayload payload) {
        super(code, message, payload);
    }

}
