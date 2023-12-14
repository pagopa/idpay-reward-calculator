package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.GENERIC_ERROR;

public class InternalServerErrorException extends ServiceException {

    public InternalServerErrorException(String message, SynchronousTransactionResponseDTO response) {
        this(GENERIC_ERROR, message,response);
    }
    public InternalServerErrorException(String code, String message, SynchronousTransactionResponseDTO response) {
        super(code, message, response);
    }

    public InternalServerErrorException(String code, String message, boolean printStackTrace, Throwable ex, SynchronousTransactionResponseDTO response) {
        super(code, message, printStackTrace, ex, response);
    }
}
