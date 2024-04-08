package it.gov.pagopa.common.web.exception;


import lombok.Getter;

@Getter
public class ServiceWithPayloadException extends ServiceException {

    private final ServiceExceptionPayload payload;

    public ServiceWithPayloadException(String code, String message, ServiceExceptionPayload payload) {
        this(code, message, payload, false, null);
    }

    public ServiceWithPayloadException(String code, String message, ServiceExceptionPayload payload, boolean printStackTrace, Throwable ex) {
        super(code,message,printStackTrace,ex);
        this.payload = payload;
    }
}
