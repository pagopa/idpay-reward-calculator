package it.gov.pagopa.reward.exception;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransactionSynchronousException extends RuntimeException{
    private final transient SynchronousTransactionResponseDTO response;

    public TransactionSynchronousException(SynchronousTransactionResponseDTO response) {
        this.response = response;
    }
}
