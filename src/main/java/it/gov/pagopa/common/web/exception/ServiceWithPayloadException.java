package it.gov.pagopa.common.web.exception;


import lombok.Getter;

@Getter
public class ServiceWithPayloadException extends RuntimeException {

    private final String code;
    private final boolean printStackTrace;
    private final ServiceExceptionPayload payload;

    public ServiceWithPayloadException(String code, String message, ServiceExceptionPayload payload) {
        this(code, message, payload, false, null);
    }

    public ServiceWithPayloadException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(message, ex);
        this.code = code;
        this.printStackTrace = printStackTrace;
        this.payload = payload;
    }
}
