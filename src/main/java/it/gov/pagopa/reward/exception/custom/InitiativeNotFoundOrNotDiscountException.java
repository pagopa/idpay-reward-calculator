package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT;

public class InitiativeNotFoundOrNotDiscountException extends ServiceException {

    public InitiativeNotFoundOrNotDiscountException(String message) {
        this(INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT, message,null);
    }
    public InitiativeNotFoundOrNotDiscountException(String message, SynchronousTransactionResponseDTO response) {
        this(INITIATIVE_NOT_FOUND_OR_NOT_DISCOUNT, message,response);
    }
    public InitiativeNotFoundOrNotDiscountException(String code, String message, SynchronousTransactionResponseDTO response) {
        super(code, message, response);
    }

}