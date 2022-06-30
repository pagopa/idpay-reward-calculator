package it.gov.pagopa.service;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.dto.RewardsTransactionDTO;


public interface TransactionService {
    RewardsTransactionDTO applyRules(TransactionDTO transaction);
}
