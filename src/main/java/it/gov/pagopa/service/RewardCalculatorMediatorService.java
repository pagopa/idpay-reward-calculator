package it.gov.pagopa.service;

import it.gov.pagopa.dto.RewardTransactionDTO;
import it.gov.pagopa.dto.TransactionDTO;

/**
 * This component will take a {@link TransactionDTO} and will calculate the {@link RewardTransactionDTO}
 * */
public interface RewardCalculatorMediatorService {
    RewardTransactionDTO execute(TransactionDTO transactionDTO);

}
