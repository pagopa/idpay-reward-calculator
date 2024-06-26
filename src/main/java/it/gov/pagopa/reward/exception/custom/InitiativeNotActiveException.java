package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.common.web.exception.ServiceWithPayloadException;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.INITIATIVE_NOT_ACTIVE_FOR_USER;


public class InitiativeNotActiveException extends ServiceWithPayloadException {
    public InitiativeNotActiveException(String message, ServiceExceptionPayload payload) {
        this(INITIATIVE_NOT_ACTIVE_FOR_USER, message,payload);
    }
    public InitiativeNotActiveException(String code, String message, ServiceExceptionPayload payload) {
        super(code, message, payload);
    }

 }
