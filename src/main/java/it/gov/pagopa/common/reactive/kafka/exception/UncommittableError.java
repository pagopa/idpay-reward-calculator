package it.gov.pagopa.common.reactive.kafka.exception;

public class UncommittableError extends RuntimeException {
    public UncommittableError(String message){
        super(message);
    }

    public UncommittableError(String message, Throwable e){
        super(message, e);
    }
}
