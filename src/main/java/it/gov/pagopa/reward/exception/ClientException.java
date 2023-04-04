package it.gov.pagopa.reward.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
@Getter
@Setter
public class ClientException extends RuntimeException{
    private final HttpStatus httpStatus;
    private final boolean printStackTrace;

    public ClientException(HttpStatus httpStatus, String message){
        this(httpStatus, message, true, null);
    }

    public ClientException(HttpStatus httpStatus, String message, Throwable ex){
        this(httpStatus, message, true, ex);
    }

    public ClientException(HttpStatus httpStatus, String message, boolean printStackTrace, Throwable ex){
        super(message, ex);
        this.httpStatus=httpStatus;
        this.printStackTrace=printStackTrace;
    }
}
