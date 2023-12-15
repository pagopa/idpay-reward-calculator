package it.gov.pagopa.reward.exception;

import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

//TODO: Check this handle
@Order(Ordered.HIGHEST_PRECEDENCE)
//@RestControllerAdvice
@Service
@Slf4j
public class ErrorManagerExtended {

 //   @ExceptionHandler(TransactionSynchronousException.class)
    public ResponseEntity<SynchronousTransactionResponseDTO> synchronousTrxHandleException(ServiceException error, ServerWebExchange exchange, ClientException clientException){
        return ResponseEntity.status(clientException.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(error.getResponse());
    }
}
