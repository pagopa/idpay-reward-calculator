package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.TransactionDTO;

public interface TransactionProcessedService {

    void saveTransactionProcessed(TransactionDTO trx);
}
