package it.gov.pagopa.reward.service.counters;

import com.fasterxml.jackson.databind.ObjectReader;
import com.mongodb.MongoException;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.connector.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static it.gov.pagopa.reward.utils.RewardConstants.PAYMENT_STATE_AUTHORIZED;
import static it.gov.pagopa.reward.utils.RewardConstants.REWARD_STATE_REJECTED;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserInitiativeCountersUnlockMediatorServiceTest {
    private static final long COMMIT_MILLIS = 10L;

    @Mock
    UserInitiativeCountersRepository userInitiativeCountersRepositoryMock;

    @Mock
    RewardErrorNotifierService rewardErrorNotifierServiceMock;

    UserInitiativeCountersUnlockMediatorServiceImpl userInitiativeCountersUnlockMediatorServiceImpl;

    @BeforeEach
    void setUp() {
        userInitiativeCountersUnlockMediatorServiceImpl = new UserInitiativeCountersUnlockMediatorServiceImpl(
                "appName",
                userInitiativeCountersRepositoryMock,
                rewardErrorNotifierServiceMock,
                COMMIT_MILLIS,
                TestUtils.objectMapper);
    }

    @Test
    void execute_statusAcceptedAuthorized(){
        RewardTransactionDTO payload = RewardTransactionDTOFaker.mockInstance(1);
        payload.setStatus(PAYMENT_STATE_AUTHORIZED);
        Message<String> message = MessageBuilder.withPayload("DUMMY PAYLOAD MESSAGE").build();
        Map<String, Object> ctx = Collections.emptyMap();

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        Mockito.when(userInitiativeCountersRepositoryMock.unlockPendingTrx(payload.getId()))
                        .thenReturn(Mono.just(userInitiativeCounters));

        UserInitiativeCounters result = userInitiativeCountersUnlockMediatorServiceImpl.execute(payload, message, ctx).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(userInitiativeCounters, result);

        Mockito.verifyNoMoreInteractions(userInitiativeCountersRepositoryMock);
    }

    @Test
    void execute_statusNotAccepted(){
        RewardTransactionDTO payload = RewardTransactionDTOFaker.mockInstance(1);
        payload.setStatus(REWARD_STATE_REJECTED);
        Message<String> message = MessageBuilder.withPayload("DUMMY PAYLOAD MESSAGE").build();
        Map<String, Object> ctx = Collections.emptyMap();


        UserInitiativeCounters result = userInitiativeCountersUnlockMediatorServiceImpl.execute(payload, message, ctx).block();

        Assertions.assertNull(result);
        Mockito.verify(userInitiativeCountersRepositoryMock, Mockito.never()).unlockPendingTrx(any());
    }

    @Test
    void execute_withException(){
        RewardTransactionDTO payload = RewardTransactionDTOFaker.mockInstance(1);
        payload.setStatus(PAYMENT_STATE_AUTHORIZED);
        Message<String> message = MessageBuilder.withPayload("DUMMY PAYLOAD MESSAGE").build();
        Map<String, Object> ctx = Collections.emptyMap();

        String errorMessage = "DUMMY MONGO EXCEPTION";
        Mockito.when(userInitiativeCountersRepositoryMock.unlockPendingTrx(payload.getId()))
                .thenThrow(new MongoException(errorMessage));

        Mono<UserInitiativeCounters> execute = userInitiativeCountersUnlockMediatorServiceImpl.execute(payload, message, ctx);
        MongoException mongoException = Assertions.assertThrows(MongoException.class, execute::block);

        Assertions.assertEquals(errorMessage, mongoException.getMessage());

        Mockito.verifyNoMoreInteractions(userInitiativeCountersRepositoryMock);
    }

    @Test
    void getCommitDelay(){
        Duration result = userInitiativeCountersUnlockMediatorServiceImpl.getCommitDelay();

        Assertions.assertEquals(Duration.ofMillis(COMMIT_MILLIS), result);
    }

    @Test
    void getObjectReader(){
        ObjectReader objectReaderResult = userInitiativeCountersUnlockMediatorServiceImpl.getObjectReader();

        Assertions.assertNotNull(objectReaderResult);
    }

    @Test
    void getFlowName(){
        String flowNameResult = userInitiativeCountersUnlockMediatorServiceImpl.getFlowName();

        Assertions.assertNotNull(flowNameResult);
        Assertions.assertEquals("USER_COUNTER_UNLOCK", flowNameResult);
    }

    @Test
    void onDeserializationError(){
        Message<String> dummyMessage = MessageBuilder.withPayload("DUMMY_MESSAGE").build();

        Consumer<Throwable> result = userInitiativeCountersUnlockMediatorServiceImpl.onDeserializationError(dummyMessage);

        Assertions.assertNotNull(result);
    }

    @Test
    void notifyError(){
        Message<String> dummyMessage = MessageBuilder.withPayload("DUMMY_MESSAGE").build();
        RuntimeException exception = new RuntimeException("DUMMY_EXCEPTION");


        Mockito.when(rewardErrorNotifierServiceMock.notifyTransactionResponse(dummyMessage, "[USER_COUNTER_UNLOCK] An error occurred evaluating transaction", true, exception))
                .thenReturn(true);

        userInitiativeCountersUnlockMediatorServiceImpl.notifyError(dummyMessage, exception);

        Mockito.verifyNoMoreInteractions(userInitiativeCountersRepositoryMock, rewardErrorNotifierServiceMock);
    }

}