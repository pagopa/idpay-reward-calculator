package it.gov.pagopa.common.web.exception;


import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    private final String code;
    private final boolean printStackTrace;
    private final transient ServiceExceptionResponse response;


    public ServiceException(String code, String message, ServiceExceptionResponse response) {
        this(code, message, false, null, response);
    }

    public ServiceException(String code, String message, boolean printStackTrace, Throwable ex, ServiceExceptionResponse response) {
        super(message, ex);
        this.code = code;
        this.printStackTrace = printStackTrace;
        this.response = response;
    }

}
