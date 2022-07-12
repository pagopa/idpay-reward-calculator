package it.gov.pagopa.service.reward.filter;

import it.gov.pagopa.dto.TransactionDTO;

import java.util.function.Predicate;

/**
 * Filter to skip transaction
 * */
public interface TransactionFilter extends Predicate<TransactionDTO> {
}
