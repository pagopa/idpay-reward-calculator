package it.gov.pagopa.common.web.exception;


import it.gov.pagopa.reward.exception.ErrorManagerExtended;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
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
    private final ErrorManagerExtended errorManagerExtended;
    private final Map<Class<? extends ServiceException>, HttpStatus> transcodeMap;

    public ServiceExceptionHandler(ErrorManager errorManager, ErrorManagerExtended errorManagerExtended, Map<Class<? extends ServiceException>, HttpStatus> transcodeMap) {
        this.errorManager = errorManager;
        this.errorManagerExtended = errorManagerExtended;
        this.transcodeMap = transcodeMap;
    }

    /**
     * Return two types of ResponseEntity:
     * <ol>
     *     <li>ErrorDTO: when ServiceException not contains a specific response</li>
     *     <li>SynchronousTransactionResponseDTO: when ServiceException contains a specific response</li>
     * </ol>
     */
    @ExceptionHandler(ServiceException.class)
    protected ResponseEntity<?> handleException(ServiceException error, ServerWebExchange exchange) {
        if(null != error.getResponse()){
            return errorManagerExtended.synchronousTrxHandleException(error,transcodeException(error));
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
}
