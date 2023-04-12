package it.gov.pagopa.reward.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
@Getter
@Setter
public class ClientExceptionNoBody extends ClientException{
    public ClientExceptionNoBody(HttpStatus httpStatus){
        this(httpStatus, null);
    }

    public ClientExceptionNoBody(HttpStatus httpStatus, Throwable ex){
        this(httpStatus, ex, true);
    }

    public ClientExceptionNoBody(HttpStatus httpStatus, Throwable ex, boolean printStackTrace){
        super(httpStatus, null, printStackTrace, ex);
    }
}
