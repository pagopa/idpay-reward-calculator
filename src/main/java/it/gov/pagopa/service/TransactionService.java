package it.gov.pagopa.service;

import it.gov.pagopa.dto.TransactionDTO;
import it.gov.pagopa.model.RewardTransaction;

public interface TransactionService {
    RewardTransaction applyRules(TransactionDTO transaction);
}
