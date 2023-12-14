package it.gov.pagopa.reward.exception;

import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class TransactionSynchronousException extends RuntimeException{
    private final HttpStatus httpStatus;
    private final transient SynchronousTransactionResponseDTO response;

    public TransactionSynchronousException(SynchronousTransactionResponseDTO response) {
        this.httpStatus = getHttpStatus(response.getRejectionReasons());
        this.response = response;
    }

    public TransactionSynchronousException(HttpStatus httpStatus, SynchronousTransactionResponseDTO response) {
        this.httpStatus = httpStatus;
        this.response = response;
    }

    private HttpStatus getHttpStatus(List<String> rejectionReasons){
        return switch(rejectionReasons.get(0)){
            case RewardConstants.TRX_REJECTION_REASON_INITIATIVE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE-> HttpStatus.FORBIDDEN;
            case RewardConstants.TRX_REJECTION_REASON_RULE_ENGINE_NOT_READY -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
