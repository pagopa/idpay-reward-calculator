package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.INITIATIVE_NOT_ACTIVE_FOR_USER;

public class InitiativeNotActiveException extends ServiceException {
    public InitiativeNotActiveException(String message, SynchronousTransactionResponseDTO response) {
        this(INITIATIVE_NOT_ACTIVE_FOR_USER, message,response);
    }
    public InitiativeNotActiveException(String code, String message, SynchronousTransactionResponseDTO response) {
        super(code, message, response);
    }

 }
