package it.gov.pagopa.reward.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ClientExceptionWithBody extends ClientException{
    private final String title;

    public ClientExceptionWithBody(HttpStatus httpStatus, String title, String message){
        this(httpStatus, title, message, null);
    }

    public ClientExceptionWithBody(HttpStatus httpStatus, String title, String message, Throwable ex){
        this(httpStatus, title, message, false, ex);
    }

    public ClientExceptionWithBody(HttpStatus httpStatus, String title, String message, boolean printStackTrace, Throwable ex){
        super(httpStatus, message, printStackTrace, ex);
        this.title = title;
    }
}
