package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.TRANSACTION_NOT_FOUND;

public class TransactioNotFoundException extends ServiceException {

    public TransactioNotFoundException(String message) {
        this(TRANSACTION_NOT_FOUND, message,null);
    }

    public TransactioNotFoundException(String code, String message, SynchronousTransactionResponseDTO response) {
        super(code, message, response);
    }

}