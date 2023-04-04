package it.gov.pagopa.reward.exception;

import it.gov.pagopa.reward.dto.ErrorDTO;
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
        defaultErrorDTO =new ErrorDTO(Severity.ERROR, "Error", "Something gone wrong");
    }
    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<ErrorDTO> handleException(RuntimeException error, ServerWebExchange exchange) {
        if(!(error instanceof ClientException clientException) || clientException.isPrintStackTrace()){
            log.error("Something gone wrong handlind request: " + exchange.getRequest().getId(), error);
        }
        if(error instanceof ClientExceptionNoBody clientExceptionNoBody){
            return ResponseEntity.status(clientExceptionNoBody.getHttpStatus()).build();
        }
        else {
            ErrorDTO errorDTO;
            HttpStatus httpStatus;
            if (error instanceof ClientExceptionWithBody clientExceptionWithBody){
                httpStatus=clientExceptionWithBody.getHttpStatus();
                errorDTO = new ErrorDTO(Severity.ERROR, clientExceptionWithBody.getTitle(),  error.getMessage());
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
}
