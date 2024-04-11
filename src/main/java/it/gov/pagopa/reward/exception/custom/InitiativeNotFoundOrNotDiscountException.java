package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceExceptionPayload;
import it.gov.pagopa.common.web.exception.ServiceWithPayloadException;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT;

public class InitiativeNotFoundOrNotDiscountException extends ServiceWithPayloadException {

    public InitiativeNotFoundOrNotDiscountException(String message, ServiceExceptionPayload payload) {
        this(INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT, message,payload);
    }
    public InitiativeNotFoundOrNotDiscountException(String code, String message, ServiceExceptionPayload payload) {
        super(code, message, payload);
    }

}