package it.gov.pagopa.service;

import it.gov.pagopa.dto.TransactionDTO;

/**
 * This component will take a {@link TransactionDTO} and will test an against all the {@link it.gov.pagopa.service.filter.TransactionFilter} configured
 * */
public interface TransactionFilterService {
    Boolean filter(TransactionDTO transactionDTO);
}
