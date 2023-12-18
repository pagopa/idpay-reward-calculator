package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.CONFLICT_ERROR;

public class RewardCalculatorConflictException extends ServiceException {

    public RewardCalculatorConflictException(SynchronousTransactionResponseDTO response) {
        this(CONFLICT_ERROR,response);
    }
    public RewardCalculatorConflictException(String message, SynchronousTransactionResponseDTO response) {
        this(CONFLICT_ERROR, message,response);
    }
    public RewardCalculatorConflictException(String code, String message, SynchronousTransactionResponseDTO response) {
        super(code, message, response);
    }

}