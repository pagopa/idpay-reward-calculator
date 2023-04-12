package it.gov.pagopa.reward.exception;

import it.gov.pagopa.reward.dto.ErrorDTO;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;


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

    @ExceptionHandler(TransactionSynchronousException.class)
    protected ResponseEntity<SynchronousTransactionResponseDTO> synchronousTrxHandleException(TransactionSynchronousException error, ServerWebExchange exchange){
        SynchronousTransactionResponseDTO response = error.getResponse();
        return ResponseEntity.status(getHttpStatus(response.getRejectionReasons()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private HttpStatus getHttpStatus(List<String> rejectionReasons){
        return switch(rejectionReasons.get(0)){
            case RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE-> HttpStatus.FORBIDDEN;
            case RewardConstants.TRX_REJECTION_ALREADY_PROCESSED -> HttpStatus.CONFLICT;
            case RewardConstants.TRX_TOO_REJECTION_TOO_MANY_REQUEST -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
