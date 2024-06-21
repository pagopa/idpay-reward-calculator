package it.gov.pagopa.reward.exception.custom;


import it.gov.pagopa.common.web.exception.ServiceException;

public class TransactionAlreadyProcessedException extends ServiceException {
    public TransactionAlreadyProcessedException(String code, String message) {
        super(code, message);
    }
}
