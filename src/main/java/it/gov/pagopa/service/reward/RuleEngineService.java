package it.gov.pagopa.service.reward;

import it.gov.pagopa.dto.RewardTransactionDTO;
import it.gov.pagopa.dto.TransactionDTO;

import java.util.List;

/**
 * This component will take {@link TransactionDTO} and the list of initiatives and will calculate the {@link RewardTransactionDTO}
 * */
public interface RuleEngineService {
    RewardTransactionDTO applyRules(TransactionDTO transaction, List<String> initiatives);
}
