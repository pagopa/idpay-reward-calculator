package it.gov.pagopa.common.web.exception;

import org.springframework.http.HttpStatus;

public class ClientExceptionNoBody extends ClientException{
    public ClientExceptionNoBody(HttpStatus httpStatus, String message) {
        super(httpStatus, message);}

    public ClientExceptionNoBody(HttpStatus httpStatus, String message, Throwable ex) {
        super(httpStatus, message, ex);
    }

    public ClientExceptionNoBody(HttpStatus httpStatus, String message, boolean printStackTrace, Throwable ex) {
        super(httpStatus, message, printStackTrace, ex);
    }
}
