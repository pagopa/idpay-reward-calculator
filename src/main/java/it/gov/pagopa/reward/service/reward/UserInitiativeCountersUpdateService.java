package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;

/**
 * This component will take {@link RewardTransactionDTO} from the Rule Engine result and update {@link UserInitiativeCounters} according to rewards
 */
public interface UserInitiativeCountersUpdateService {
    UserInitiativeCounters update(UserInitiativeCounters userInitiativeCounters, RewardTransactionDTO ruleEngineResult);
}
