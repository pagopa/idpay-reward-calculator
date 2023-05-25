package it.gov.pagopa.common.web.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClientException extends RuntimeException{
    private final HttpStatus httpStatus;
    private final boolean printStackTrace;

    public ClientException(HttpStatus httpStatus, String message){
        this(httpStatus, message, null);
    }

    public ClientException(HttpStatus httpStatus, String message, Throwable ex){
        this(httpStatus, message, false, ex);
    }

    public ClientException(HttpStatus httpStatus, String message, boolean printStackTrace, Throwable ex){
        super(message, ex);
        this.httpStatus=httpStatus;
        this.printStackTrace=printStackTrace;
    }
}
