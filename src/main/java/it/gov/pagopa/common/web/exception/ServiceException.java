package it.gov.pagopa.common.web.exception;


import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final String code;
    private final boolean printStackTrace;
    private final ServiceExceptionResponse response;


    public ServiceException(String code, String message, ServiceExceptionResponse response) {
        this(code, message, response, false, null);
    }

    public ServiceException(String code, String message, ServiceExceptionResponse response, boolean printStackTrace, Throwable ex) {
        super(message, ex);
        this.code = code;
        this.printStackTrace = printStackTrace;
        this.response = response;
    }

}
