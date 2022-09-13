package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.TransactionDTO;

public interface TransactionValidatorService {
    /** it will fill the {@link TransactionDTO#getRejectionReasons()} in caso of invalid transaction */
    TransactionDTO validate(TransactionDTO transactionDTO);
}
