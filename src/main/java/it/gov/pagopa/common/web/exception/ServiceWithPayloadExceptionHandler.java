package it.gov.pagopa.common.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceWithPayloadExceptionHandler {
    private final Map<Class<? extends ServiceWithPayloadException>, HttpStatus> transcodeMap;

    public ServiceWithPayloadExceptionHandler(Map<Class<? extends ServiceWithPayloadException>, HttpStatus> transcodeMap) {
        this.transcodeMap = transcodeMap;
    }

    @ExceptionHandler(ServiceWithPayloadException.class)
    protected ResponseEntity<ServiceExceptionPayload> handleException(ServiceWithPayloadException error, ServerWebExchange exchange) {
        return handleBodyProvidedException(error, exchange);
    }

    private ClientException transcodeException(ServiceWithPayloadException error) {
        HttpStatus httpStatus = transcodeMap.get(error.getClass());

        if (httpStatus == null) {
            log.warn("Unhandled exception: {}", error.getClass().getName());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ClientExceptionWithBody(httpStatus, error.getCode(), error.getMessage(), error.isPrintStackTrace(), error);
    }

    private ResponseEntity<ServiceExceptionPayload> handleBodyProvidedException(ServiceWithPayloadException error, ServerWebExchange exchange) {
        ClientException clientException = transcodeException(error);
        ErrorManager.logClientException(clientException, exchange);

        return ResponseEntity.status(clientException.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(error.getPayload());
    }
}
