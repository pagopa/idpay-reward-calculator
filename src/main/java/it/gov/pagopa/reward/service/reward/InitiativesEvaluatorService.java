package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;

import java.util.List;

/**
 * It will evaluate all the input initiatives on the current transaction,
 * using the counters to evaluate the current state of budget
 */
public interface InitiativesEvaluatorService {

    RewardTransactionDTO evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCounters userCounters);
}
