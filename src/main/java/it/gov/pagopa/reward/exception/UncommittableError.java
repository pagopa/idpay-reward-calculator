package it.gov.pagopa.reward.exception;

public class UncommittableError extends RuntimeException {
    public UncommittableError(String message){
        super(message);
    }
}
