package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.service.reward.filter.TransactionFilter;

/**
 * This component will take a {@link TransactionDTO} and will test an against all the {@link TransactionFilter} configured
 * */
public interface TransactionFilterService {
    Boolean filter(TransactionDTO transactionDTO);
}
