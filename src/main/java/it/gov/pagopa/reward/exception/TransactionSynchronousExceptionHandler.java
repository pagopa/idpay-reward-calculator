package it.gov.pagopa.reward.exception;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class TransactionSynchronousExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<SynchronousTransactionResponseDTO> handleException(RuntimeException error){
        SynchronousTransactionResponseDTO response;
        HttpStatus httpStatus;
        if (error instanceof TransactionSynchronousException transactionSynchronousException) {
            httpStatus = determinateStatus(transactionSynchronousException.getResponse().getRejectionReasons());
            response = transactionSynchronousException.getResponse();

        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            response = null;
        }
        return ResponseEntity.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    private HttpStatus determinateStatus(List<String> rejectionReasons){
        return RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND
                .equals(rejectionReasons.get(0)) ? HttpStatus.NOT_FOUND: HttpStatus.FORBIDDEN;
    }
}
