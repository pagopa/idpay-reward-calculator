package it.gov.pagopa.common.kafka.exception;

public class UncommittableError extends RuntimeException {
    public UncommittableError(String message){
        super(message);
    }
}
