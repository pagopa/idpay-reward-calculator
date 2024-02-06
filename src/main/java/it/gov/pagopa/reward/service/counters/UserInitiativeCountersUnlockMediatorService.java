package it.gov.pagopa.reward.service.counters;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 *  This component will take a {@link RewardTransactionDTO} and unlock the {@link it.gov.pagopa.reward.model.counters.UserInitiativeCounters}
 * */
public interface UserInitiativeCountersUnlockMediatorService {
    void execute(Flux<Message<String>> transactionDTO);
}
