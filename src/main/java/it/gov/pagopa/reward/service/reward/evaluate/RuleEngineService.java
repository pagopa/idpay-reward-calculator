package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;

import java.util.List;

/**
 * This component will take {@link TransactionDTO} and the list of initiatives and will calculate the {@link RewardTransactionDTO}
 * */
public interface RuleEngineService {
    RewardTransactionDTO applyRules(TransactionDTO transaction, List<String> initiatives, UserInitiativeCounters userInitiativeCounters);
}
