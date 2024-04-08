package it.gov.pagopa.common.web.exception;


import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final String code;
    private final boolean printStackTrace;


    public ServiceException(String code, String message) {
        this(code, message, false, null);
    }

    public ServiceException(String code, String message, boolean printStackTrace, Throwable ex) {
        super(message, ex);
        this.code = code;
        this.printStackTrace = printStackTrace;
    }

}
