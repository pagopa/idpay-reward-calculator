package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import reactor.core.publisher.Mono;

/**
 * This component will take {@link RewardTransactionDTO} from the Rule Engine result and update {@link UserInitiativeCounters} according to rewards
 */
public interface UserInitiativeCountersUpdateService {
    Mono<RewardTransactionDTO> update(UserInitiativeCounters userInitiativeCounters, RewardTransactionDTO ruleEngineResult);
}
