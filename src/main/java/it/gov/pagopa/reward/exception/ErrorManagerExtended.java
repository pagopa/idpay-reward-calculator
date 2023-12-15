package it.gov.pagopa.reward.exception;

import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class ErrorManagerExtended {

    public ResponseEntity<SynchronousTransactionResponseDTO> synchronousTrxHandleException(ServiceException error, ClientException clientException){
        return ResponseEntity.status(clientException.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(error.getResponse());
    }
}
