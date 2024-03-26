package it.gov.pagopa.reward.service.counters;

import com.mongodb.MongoException;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.synchronous.op.CancelTrxSynchronousServiceImpl;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static it.gov.pagopa.reward.utils.RewardConstants.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserInitiativeCountersUnlockMediatorServiceTest {
    @Mock
    UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;

    UserInitiativeCountersUnlockMediatorServiceImpl userInitiativeCountersUnlockMediatorServiceImpl;
    @Mock
    CancelTrxSynchronousServiceImpl cancelTrxSynchronousService;

    @BeforeEach
    void setUp() {
        userInitiativeCountersUnlockMediatorServiceImpl = new UserInitiativeCountersUnlockMediatorServiceImpl(
                userInitiativeCountersRepositoryMock, cancelTrxSynchronousService);


    }

    @ParameterizedTest
    @ValueSource(strings = {PAYMENT_STATE_AUTHORIZED, PAYMENT_STATE_REWARDED})
    void execute_statusAuthorizedAndRewarded(String statusAccepted){
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
        trx.setStatus(statusAccepted);
        trx.setChannel(TRX_CHANNEL_QRCODE);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        Mockito.when(userInitiativeCountersRepositoryMock.unlockPendingTrx(trx.getId()))
                        .thenReturn(Mono.just(userInitiativeCounters));

        UserInitiativeCounters result = userInitiativeCountersUnlockMediatorServiceImpl.execute(trx).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(userInitiativeCounters, result);

        Mockito.verifyNoMoreInteractions(userInitiativeCountersRepositoryMock);
    }

    @ParameterizedTest
    @ValueSource(strings = {PAYMENT_STATE_REJECTED})
    void execute_statusRejected(String statusAccepted){
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
        trx.setStatus(statusAccepted);
        trx.setChannel(TRX_CHANNEL_QRCODE);

        String initiativeId = trx.getInitiatives().get(0);
        Reward rewardInitiative = trx.getRewards().get(trx.getInitiatives().get(0));
        Long rewardCents = CommonUtilities.euroToCents(rewardInitiative.getAccruedReward());
        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(trx.getUserId(), InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);

        userInitiativeCounters.setPendingTrx(trx);
        Mockito.when(userInitiativeCountersRepositoryMock.findByPendingTrx(trx.getId()))
                .thenReturn(Mono.just(userInitiativeCounters));
        userInitiativeCounters.setPendingTrx(null);
        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(userInitiativeCounters.getEntityId(),
                new HashMap<>(Map.of(initiativeId,
                        userInitiativeCounters)));
        Mockito.when(userInitiativeCountersRepositoryMock.saveIfVersionNotChanged(userInitiativeCounters)).thenReturn(Mono.just(userInitiativeCounters));

        Pair<UserInitiativeCountersWrapper,RewardTransactionDTO> pair = Pair.of(userInitiativeCountersWrapper,trx);
        Mono<Pair<UserInitiativeCountersWrapper,RewardTransactionDTO>> monoPair = Mono.just(pair);

        Mockito.when(cancelTrxSynchronousService.handleUnlockedCounterForRefundTrx("USER_COUNTER_UNLOCK", trx, initiativeId, userInitiativeCountersWrapper, rewardCents)).thenReturn(monoPair);

        UserInitiativeCounters result = userInitiativeCountersUnlockMediatorServiceImpl.execute(trx).block();


        Assertions.assertNotNull(result);
        Assertions.assertEquals(userInitiativeCounters, result);

        Mockito.verifyNoMoreInteractions(userInitiativeCountersRepositoryMock);
    }

    @ParameterizedTest
    @ValueSource(strings = {PAYMENT_STATE_REJECTED})
    void execute_statusRejectedAmountCentsNull(String statusAccepted){
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
        trx.setStatus(statusAccepted);
        trx.setChannel(TRX_CHANNEL_QRCODE);
        trx.setAmountCents(null);

        String errorMessage = "The trx with id %s has amountCents not valid".formatted(trx.getId());

        Mono<UserInitiativeCounters> execute = userInitiativeCountersUnlockMediatorServiceImpl.execute(trx);
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,execute::block);
        Assertions.assertEquals(errorMessage,exception.getMessage());
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.never()).findByPendingTrx(any());

    }

    @Test
    void execute_channelNotAccepted(){
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
        trx.setStatus(REWARD_STATE_REJECTED);
        trx.setChannel(TRX_CHANNEL_RTD);

        UserInitiativeCounters result = userInitiativeCountersUnlockMediatorServiceImpl.execute(trx).block();

        Assertions.assertNull(result);
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.never()).unlockPendingTrx(any());
    }

    @Test
    void execute_statusNotAccepted(){
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
        trx.setStatus("REFUND");
        trx.setChannel(TRX_CHANNEL_QRCODE);

        UserInitiativeCounters result = userInitiativeCountersUnlockMediatorServiceImpl.execute(trx).block();

        Assertions.assertNull(result);
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.never()).unlockPendingTrx(any());
    }

    @Test
    void execute_withException(){
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
        trx.setStatus(PAYMENT_STATE_AUTHORIZED);
        trx.setChannel(TRX_CHANNEL_QRCODE);


        String errorMessage = "DUMMY MONGO EXCEPTION";
        Mockito.when(userInitiativeCountersRepositoryMock.unlockPendingTrx(trx.getId()))
                .thenThrow(new MongoException(errorMessage));

        Mono<UserInitiativeCounters> execute = userInitiativeCountersUnlockMediatorServiceImpl.execute(trx);
        MongoException mongoException = Assertions.assertThrows(MongoException.class, execute::block);

        Assertions.assertEquals(errorMessage, mongoException.getMessage());

        Mockito.verifyNoMoreInteractions(userInitiativeCountersRepositoryMock);
    }

}