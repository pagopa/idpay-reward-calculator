package it.gov.pagopa.reward.exception;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;


@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class ErrorManagerExtended {

    @ExceptionHandler(TransactionSynchronousException.class)
    protected ResponseEntity<SynchronousTransactionResponseDTO> synchronousTrxHandleException(TransactionSynchronousException error, ServerWebExchange exchange){
        return ResponseEntity.status(error.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(error.getResponse());
    }
}
