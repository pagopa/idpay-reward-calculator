package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;


@RestControllerAdvice
@Slf4j
public class ErrorManager {
    private final ErrorDTO defaultErrorDTO;

    public ErrorManager(@Nullable ErrorDTO defaultErrorDTO) {
        this.defaultErrorDTO = Optional.ofNullable(defaultErrorDTO)
                .orElse(new ErrorDTO("Error", "Something gone wrong"));
    }
    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<ErrorDTO> handleException(RuntimeException error, ServerWebExchange exchange) {
        logClientException(error, exchange);

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

    public static void logClientException(RuntimeException error, ServerWebExchange exchange) {
        Throwable unwrappedException = error.getCause() instanceof ServiceException
                ? error.getCause()
                : error;

        String clientExceptionMessage = "";
        if(error instanceof ClientException clientException) {
            clientExceptionMessage = ": HttpStatus %s - %s%s".formatted(
                    clientException.getHttpStatus(),
                    (clientException instanceof ClientExceptionWithBody clientExceptionWithBody) ? clientExceptionWithBody.getCode() + ": " : "",
                    clientException.getMessage()
            );
        }

        if(!(error instanceof ClientException clientException) || clientException.isPrintStackTrace() || unwrappedException.getCause() != null){
            log.error("Something went wrong handling request {}{}", getRequestDetails(exchange), clientExceptionMessage, unwrappedException);
        } else {
            log.info("A {} occurred handling request {}{} at {}",
                    unwrappedException.getClass().getSimpleName() ,
                    getRequestDetails(exchange),
                    clientExceptionMessage,
                    unwrappedException.getStackTrace().length > 0 ? unwrappedException.getStackTrace()[0] : "UNKNOWN");
        }
    }

    public static String getRequestDetails(ServerWebExchange exchange) {
        return "%s %s (%s)".formatted(exchange.getRequest().getMethod(), exchange.getRequest().getURI(), exchange.getRequest().getId());
    }
}
