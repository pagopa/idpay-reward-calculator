package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;

import java.util.List;

/**
 * It will evaluate all the input initiatives on the current transaction,
 * using the counters to evaluate the current state of budget
 */
public interface InitiativesEvaluatorService {

    RewardTransactionDTO evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCountersWrapper userCounters);
}
