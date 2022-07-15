package it.gov.pagopa.reward.service.reward.filter;

import it.gov.pagopa.reward.dto.TransactionDTO;

import java.util.function.Predicate;

/**
 * Filter to skip transaction
 * */
public interface TransactionFilter extends Predicate<TransactionDTO> {
}
