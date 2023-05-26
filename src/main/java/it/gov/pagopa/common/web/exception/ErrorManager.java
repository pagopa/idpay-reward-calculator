package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;


@RestControllerAdvice
@Slf4j
public class ErrorManager {
    private static final ErrorDTO defaultErrorDTO;
    static {
        defaultErrorDTO =new ErrorDTO("Error", "Something gone wrong");
    }
    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<ErrorDTO> handleException(RuntimeException error, ServerWebExchange exchange) {
        if(!(error instanceof ClientException clientException) || clientException.isPrintStackTrace() || clientException.getCause() != null){
            log.error("Something went wrong handling request {}", getRequestDetails(exchange), error);
        } else {
            log.info("A {} occurred handling request {}: HttpStatus {} - {} at {}",
                    clientException.getClass().getSimpleName(),
                    getRequestDetails(exchange),
                    clientException.getHttpStatus(),
                    clientException.getMessage(),
                    clientException.getStackTrace().length > 0 ? clientException.getStackTrace()[0] : "UNKNOWN");
        }

        if(error instanceof ClientExceptionNoBody clientExceptionNoBody){
            return ResponseEntity.status(clientExceptionNoBody.getHttpStatus()).build();
        }
        else {
            ErrorDTO errorDTO;
            HttpStatus httpStatus;
            if (error instanceof ClientExceptionWithBody clientExceptionWithBody){
                httpStatus=clientExceptionWithBody.getHttpStatus();
                errorDTO = new ErrorDTO(clientExceptionWithBody.getCode(),  error.getMessage());
            }
            else {
                httpStatus=HttpStatus.INTERNAL_SERVER_ERROR;
                errorDTO = defaultErrorDTO;
            }
            return ResponseEntity.status(httpStatus)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorDTO);
        }
    }

    public static String getRequestDetails(ServerWebExchange exchange) {
        return "%s %s (%s)".formatted(exchange.getRequest().getMethod(), exchange.getRequest().getURI(), exchange.getRequest().getId());
    }
}
