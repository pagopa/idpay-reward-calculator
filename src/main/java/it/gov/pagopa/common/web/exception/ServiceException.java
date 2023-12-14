package it.gov.pagopa.common.web.exception;


import it.gov.pagopa.reward.dto.synchronous.SynchronousTransactionResponseDTO;
import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    private final String code;
    private final boolean printStackTrace;
    private final transient SynchronousTransactionResponseDTO response;


    public ServiceException(String code, String message, SynchronousTransactionResponseDTO response) {
        this(code, message, false, null, response);
    }

    public ServiceException(String code, String message, boolean printStackTrace, Throwable ex, SynchronousTransactionResponseDTO response) {
        super(message, ex);
        this.code = code;
        this.printStackTrace = printStackTrace;
        this.response = response;
    }

}
