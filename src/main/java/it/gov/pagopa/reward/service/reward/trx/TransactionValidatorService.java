package it.gov.pagopa.reward.service.reward.trx;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;

public interface TransactionValidatorService {
    /** it will fill the {@link TransactionDTO#getRejectionReasons()} in caso of invalid transaction */
    TransactionDTO validate(TransactionDTO transactionDTO);
}
