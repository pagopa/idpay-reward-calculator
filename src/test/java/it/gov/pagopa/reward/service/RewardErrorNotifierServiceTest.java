package it.gov.pagopa.reward.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class RewardErrorNotifierServiceTest {
    private static final String BINDER_KAFKA_TYPE="kafka";
    private static final String BINDER_BROKER="broker";
    private static final String DUMMY_MESSAGE="DUMMY MESSAGE";

    private static final String TRX_RESPONSE_TOPIC = "trx-response-topic";
    private static final String TRX_RESPONSE_GROUP = "trx-response-group";

    private static final Message<String> dummyMessage = MessageBuilder.withPayload(DUMMY_MESSAGE).build();

    @Mock
    private ErrorNotifierService errorNotifierServiceMock;

    private RewardErrorNotifierService rewardErrorNotifierService;

    @BeforeEach
    void setUp() {
        rewardErrorNotifierService = new RewardErrorNotifierServiceImpl(
                errorNotifierServiceMock,
                BINDER_KAFKA_TYPE,
                BINDER_BROKER,

                "reward-rule-topic",
                "rewrd-rule-group",
                BINDER_KAFKA_TYPE,
                BINDER_BROKER,

                "trx-topic",
                "trx-Group",

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                "trx-reward-topic",

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                "hpan-update-topic",
                "hpan-update-group",

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                "hpanUpdateOutcome-topic",

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                "commands-topic",
                "commands-group",

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                TRX_RESPONSE_TOPIC,
                TRX_RESPONSE_GROUP
        );
    }

    @Test
    void notifyTransactionResponse() {

        errorNotifyMock(TRX_RESPONSE_TOPIC, TRX_RESPONSE_GROUP, true, true );

        boolean result = rewardErrorNotifierService.notifyTransactionResponse(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        Assertions.assertTrue(result);
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    private void errorNotifyMock(String topic, String group, boolean retryable, boolean resendApplication ) {
        Mockito.when(errorNotifierServiceMock.notify(eq(BINDER_KAFKA_TYPE), eq(BINDER_BROKER),
                eq(topic), eq(group), eq(dummyMessage), eq(DUMMY_MESSAGE), eq(retryable), eq(resendApplication), any()))
                .thenReturn(true);
    }
}