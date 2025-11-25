package it.gov.pagopa.reward.service.counters;

import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.repository.primary.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.synchronous.op.CancelTrxSynchronousServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.reward.utils.RewardConstants.*;

@Service
@Slf4j
public class UserInitiativeCountersUnlockMediatorServiceImpl implements UserInitiativeCountersUnlockMediatorService {

    private static final List<String> ACCEPTED_STATUS = List.of(PAYMENT_STATE_AUTHORIZED, PAYMENT_STATE_REWARDED,PAYMENT_STATE_REJECTED);

    private static final List<String> ACCEPTED_CHANNEL = List.of(TRX_CHANNEL_QRCODE, TRX_CHANNEL_BARCODE,TRX_CHANNEL_IDPAYCODE);
    private final UserInitiativeCountersRepository userInitiativeCountersRepository;
    private final CancelTrxSynchronousServiceImpl cancelTrxSynchronousService;


    public UserInitiativeCountersUnlockMediatorServiceImpl(UserInitiativeCountersRepository userInitiativeCountersRepository, CancelTrxSynchronousServiceImpl cancelTrxSynchronousService){
        this.userInitiativeCountersRepository = userInitiativeCountersRepository;

        this.cancelTrxSynchronousService = cancelTrxSynchronousService;
    }

    @Override
    public Mono<UserInitiativeCounters> execute(RewardTransactionDTO transactionDTO) {
        log.info("[USER_COUNTER_UNLOCK] Start handle unlock counter on trxId {}",transactionDTO.getId());
        return Mono.just(transactionDTO)
                .filter(trx -> ACCEPTED_CHANNEL.contains(trx.getChannel()) && ACCEPTED_STATUS.contains(trx.getStatus()))
                .flatMap(this::handlerUnlockType);
    }

    private Mono<UserInitiativeCounters> handlerUnlockType(RewardTransactionDTO trx) {
        log.info("[USER_COUNTER_UNLOCK] Handle unlock counter on trxId {} with status {} and channel {}",trx.getId(),trx.getStatus(),trx.getChannel());
        if(PAYMENT_STATE_AUTHORIZED.equals(trx.getStatus())
            || PAYMENT_STATE_REWARDED.equals(trx.getStatus())) {
            if (trx.getFamilyId() != null) {
                return userInitiativeCountersRepository.unlockPendingTrxById(trx.getFamilyId()+
                "-"+trx.getInitiativeId());
            }
            return userInitiativeCountersRepository.unlockPendingTrx(trx.getId());
        }
        return handleUnlockedRejectedType(trx);
    }
    private Mono<UserInitiativeCounters> handleUnlockedRejectedType(RewardTransactionDTO trx) {
        if(null == trx.getAmountCents()){
            return Mono.error(new IllegalStateException("The trx with id %s has amountCents not valid".formatted(trx.getId())));
        }
        trx.setAmount(CommonUtilities.centsToEuro(trx.getAmountCents()));
        return userInitiativeCountersRepository.findByPendingTrx(trx.getId())
                .flatMap(userInitiativeCounters -> {
                    String initiativeId = trx.getInitiatives().get(0);
                    Reward rewardInitiative = trx.getRewards().get(initiativeId);
                    Long rewardCents = rewardInitiative.getAccruedRewardCents();
                    cancelTrxSynchronousService.transformIntoRefundTrx(
                            trx,
                            initiativeId,
                            rewardInitiative.getOrganizationId(),
                            rewardCents
                    );

                    userInitiativeCounters.setPendingTrx(null);
                    UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(userInitiativeCounters.getEntityId(),
                            new HashMap<>(Map.of(initiativeId,
                                    userInitiativeCounters)));

                    return cancelTrxSynchronousService.handleUnlockedCounterForRefundTrx("USER_COUNTER_UNLOCK", trx, initiativeId, userInitiativeCountersWrapper, rewardCents)
                            .flatMap(counter2rewardTrx -> userInitiativeCountersRepository.saveIfVersionNotChanged(userInitiativeCounters));

                });
    }

}
