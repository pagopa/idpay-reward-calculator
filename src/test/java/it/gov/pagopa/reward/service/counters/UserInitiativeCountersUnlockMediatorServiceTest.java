package it.gov.pagopa.reward.service.counters;

import com.mongodb.MongoException;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
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
import reactor.core.publisher.Mono;

import static it.gov.pagopa.reward.utils.RewardConstants.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserInitiativeCountersUnlockMediatorServiceTest {
    @Mock
    UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;

    UserInitiativeCountersUnlockMediatorServiceImpl userInitiativeCountersUnlockMediatorServiceImpl;

    @BeforeEach
    void setUp() {
        userInitiativeCountersUnlockMediatorServiceImpl = new UserInitiativeCountersUnlockMediatorServiceImpl(
                userInitiativeCountersRepositoryMock);
    }

    @ParameterizedTest
    @ValueSource(strings = {PAYMENT_STATE_AUTHORIZED, PAYMENT_STATE_REWARDED})
    void execute_statusAccepted(String statusAccepted){
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
        trx.setStatus(statusAccepted);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        Mockito.when(userInitiativeCountersRepositoryMock.unlockPendingTrx(trx.getId()))
                        .thenReturn(Mono.just(userInitiativeCounters));

        UserInitiativeCounters result = userInitiativeCountersUnlockMediatorServiceImpl.execute(trx).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(userInitiativeCounters, result);

        Mockito.verifyNoMoreInteractions(userInitiativeCountersRepositoryMock);
    }

    @Test
    void execute_statusNotAccepted(){
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
        trx.setStatus(REWARD_STATE_REJECTED);

        UserInitiativeCounters result = userInitiativeCountersUnlockMediatorServiceImpl.execute(trx).block();

        Assertions.assertNull(result);
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.never()).unlockPendingTrx(any());
    }

    @Test
    void execute_withException(){
        RewardTransactionDTO trx = RewardTransactionDTOFaker.mockInstance(1);
        trx.setStatus(PAYMENT_STATE_AUTHORIZED);


        String errorMessage = "DUMMY MONGO EXCEPTION";
        Mockito.when(userInitiativeCountersRepositoryMock.unlockPendingTrx(trx.getId()))
                .thenThrow(new MongoException(errorMessage));

        Mono<UserInitiativeCounters> execute = userInitiativeCountersUnlockMediatorServiceImpl.execute(trx);
        MongoException mongoException = Assertions.assertThrows(MongoException.class, execute::block);

        Assertions.assertEquals(errorMessage, mongoException.getMessage());

        Mockito.verifyNoMoreInteractions(userInitiativeCountersRepositoryMock);
    }

}