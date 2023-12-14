package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.INITIATIVE_NOT_READY;

public class InitiativeNotInContainerException  extends ServiceException {

    public InitiativeNotInContainerException(String message, SynchronousTransactionResponseDTO response) {
        this(INITIATIVE_NOT_READY, message,response);
    }
    public InitiativeNotInContainerException(String code, String message, SynchronousTransactionResponseDTO response) {
        super(code, message, response);
    }

    public InitiativeNotInContainerException(String code, String message, boolean printStackTrace, Throwable ex, SynchronousTransactionResponseDTO response) {
        super(code, message, printStackTrace, ex, response);
    }
}
