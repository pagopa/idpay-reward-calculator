package it.gov.pagopa.reward.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;

import static it.gov.pagopa.reward.utils.RewardConstants.ExceptionCode.TRANSACTION_ASSIGNED_TO_ANOTHER_USER;

public class TransactionAssignedToAnotherUserException extends ServiceException {

    public TransactionAssignedToAnotherUserException(String message) {
        this(TRANSACTION_ASSIGNED_TO_ANOTHER_USER, message);
    }
    public TransactionAssignedToAnotherUserException(String code, String message) {
        super(code, message, null);
    }

}