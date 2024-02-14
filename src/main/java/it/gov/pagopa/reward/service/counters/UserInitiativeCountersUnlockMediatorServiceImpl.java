package it.gov.pagopa.reward.service.counters;

import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.gov.pagopa.reward.utils.RewardConstants.PAYMENT_STATE_AUTHORIZED;

@Service
@Slf4j
public class UserInitiativeCountersUnlockMediatorServiceImpl implements UserInitiativeCountersUnlockMediatorService {

    private static final List<String> ACCEPTED_STATUS = List.of(PAYMENT_STATE_AUTHORIZED);
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;

    public UserInitiativeCountersUnlockMediatorServiceImpl(UserInitiativeCountersRepository userInitiativeCountersRepository){
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;

    }

    @Override
    public Mono<UserInitiativeCounters> execute(RewardTransactionDTO transactionDTO) {
        return Mono.just(transactionDTO)
                .filter(trx -> ACCEPTED_STATUS.contains(trx.getStatus()))
                .flatMap(this::handlerUnlockType);
    }

    private Mono<UserInitiativeCounters> handlerUnlockType(RewardTransactionDTO trx) {
        if(PAYMENT_STATE_AUTHORIZED.equals(trx.getStatus())) {
            return userInitiativeCountersRepository.unlockPendingTrx(trx.getId());
        }
        //TODO handle expired event (CANCELED status)
        return Mono.empty();
    }
}
