package it.gov.pagopa.reward.connector.event.consumer;

import com.fasterxml.jackson.databind.ObjectReader;
import com.mongodb.MongoException;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.RewardErrorNotifierService;
import it.gov.pagopa.reward.service.counters.UserInitiativeCountersUnlockMediatorService;
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

@ExtendWith(MockitoExtension.class)
class TransactionConsumerTest {
    private static final long COMMIT_MILLIS = 10L;

    @Mock
    UserInitiativeCountersUnlockMediatorService userInitiativeCountersUnlockMediatorServiceMock;

    @Mock
    RewardErrorNotifierService rewardErrorNotifierServiceMock;

    TransactionConsumerImpl transactionConsumer;

    @BeforeEach
    void setUp() {
        transactionConsumer = new TransactionConsumerImpl(
                "appName",
                rewardErrorNotifierServiceMock,
                userInitiativeCountersUnlockMediatorServiceMock,
                COMMIT_MILLIS,
                TestUtils.objectMapper);
    }

    @Test
    void executeTest(){
        RewardTransactionDTO payload = RewardTransactionDTOFaker.mockInstance(1);
        payload.setStatus(PAYMENT_STATE_AUTHORIZED);
        Message<String> message = MessageBuilder.withPayload("DUMMY PAYLOAD MESSAGE").build();
        Map<String, Object> ctx = Collections.emptyMap();

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        Mockito.when(userInitiativeCountersUnlockMediatorServiceMock.execute(payload))
                .thenReturn(Mono.just(userInitiativeCounters));

        UserInitiativeCounters result = transactionConsumer.execute(payload, message, ctx).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(userInitiativeCounters, result);

        Mockito.verifyNoMoreInteractions(userInitiativeCountersUnlockMediatorServiceMock);
    }

    @Test
    void execute_withException(){
        RewardTransactionDTO payload = RewardTransactionDTOFaker.mockInstance(1);
        payload.setStatus(PAYMENT_STATE_AUTHORIZED);
        Message<String> message = MessageBuilder.withPayload("DUMMY PAYLOAD MESSAGE").build();
        Map<String, Object> ctx = Collections.emptyMap();

        String errorMessage = "DUMMY MONGO EXCEPTION";
        Mockito.when(userInitiativeCountersUnlockMediatorServiceMock.execute(payload))
                .thenThrow(new MongoException(errorMessage));

        Assertions.assertThrows(MongoException.class, () -> transactionConsumer.execute(payload, message, ctx));


        Mockito.verifyNoMoreInteractions(userInitiativeCountersUnlockMediatorServiceMock);
    }

    @Test
    void getCommitDelay(){
        Duration result = transactionConsumer.getCommitDelay();

        Assertions.assertEquals(Duration.ofMillis(COMMIT_MILLIS), result);
    }

    @Test
    void getObjectReader(){
        ObjectReader objectReaderResult = transactionConsumer.getObjectReader();

        Assertions.assertNotNull(objectReaderResult);
    }

    @Test
    void getFlowName(){
        String flowNameResult = transactionConsumer.getFlowName();

        Assertions.assertNotNull(flowNameResult);
        Assertions.assertEquals("USER_COUNTER_UNLOCK", flowNameResult);
    }

    @Test
    void onDeserializationError(){
        Message<String> dummyMessage = MessageBuilder.withPayload("DUMMY_MESSAGE").build();

        Consumer<Throwable> result = transactionConsumer.onDeserializationError(dummyMessage);

        Assertions.assertNotNull(result);
    }

    @Test
    void notifyError(){
        Message<String> dummyMessage = MessageBuilder.withPayload("DUMMY_MESSAGE").build();
        RuntimeException exception = new RuntimeException("DUMMY_EXCEPTION");


        Mockito.when(rewardErrorNotifierServiceMock.notifyTransactionResponse(dummyMessage, "[USER_COUNTER_UNLOCK] An error occurred evaluating transaction", true, exception))
                .thenReturn(true);

        transactionConsumer.notifyError(dummyMessage, exception);

        Mockito.verifyNoMoreInteractions(userInitiativeCountersUnlockMediatorServiceMock, rewardErrorNotifierServiceMock);
    }

}