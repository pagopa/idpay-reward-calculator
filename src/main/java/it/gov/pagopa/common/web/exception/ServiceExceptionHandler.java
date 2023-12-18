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
public class ServiceExceptionHandler {
    private final ErrorManager errorManager;
    private final Map<Class<? extends ServiceException>, HttpStatus> transcodeMap;

    public ServiceExceptionHandler(ErrorManager errorManager, Map<Class<? extends ServiceException>, HttpStatus> transcodeMap) {
        this.errorManager = errorManager;
        this.transcodeMap = transcodeMap;
    }
    @SuppressWarnings("squid:S1452")
    @ExceptionHandler(ServiceException.class)
    protected ResponseEntity<? extends ServiceExceptionResponse> handleException(ServiceException error, ServerWebExchange exchange) {
        if(null != error.getResponse()){
            return handleBodyProvidedException(error,transcodeException(error));
        }
        return errorManager.handleException(transcodeException(error), exchange);
    }

    private ClientException transcodeException(ServiceException error) {
        HttpStatus httpStatus = transcodeMap.get(error.getClass());

        if (httpStatus == null) {
            log.warn("Unhandled exception: {}", error.getClass().getName());
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ClientExceptionWithBody(httpStatus, error.getCode(), error.getMessage(), error.getCause());
    }

    private ResponseEntity<? extends ServiceExceptionResponse> handleBodyProvidedException(ServiceException error, ClientException clientException){
        return ResponseEntity.status(clientException.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(error.getResponse());
    }
}
