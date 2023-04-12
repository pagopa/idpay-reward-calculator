package it.gov.pagopa.reward.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class ClientExceptionWithBody extends ClientException{
    private final String title;

    public ClientExceptionWithBody(HttpStatus httpStatus, String title, String message){
        this(httpStatus, title, message, true, null);
    }

    public ClientExceptionWithBody(HttpStatus httpStatus, String title, String message, Throwable ex){
        this(httpStatus, title, message, true, ex);
    }

    public ClientExceptionWithBody(HttpStatus httpStatus, String title, String message, boolean printStackTrace, Throwable ex){
        super(httpStatus, message, printStackTrace, ex);
        this.title = title;
    }
}
