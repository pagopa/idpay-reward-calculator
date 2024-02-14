package it.gov.pagopa.reward.service.counters;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import reactor.core.publisher.Mono;

/**
 *  This component will take a {@link RewardTransactionDTO} and unlock the {@link it.gov.pagopa.reward.model.counters.UserInitiativeCounters}
 * */
public interface UserInitiativeCountersUnlockMediatorService {
    Mono<UserInitiativeCounters> execute(RewardTransactionDTO transactionDTO);
}
