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
    private static final String REWARD_RULE_TOPIC = "reward-rule-topic";
    private static final String REWARD_RULE_GROUP = "rewrd-rule-group";
    private static final String TRANSACTIONS_TOPIC = "trx-topic";
    private static final String TRANSACTIONS_GROUP = "trx-Group";
    private static final String TRANSACTION_REWARDED_TOPIC = "trx-reward-topic";
    private static final String HPAN_UPDATE_TOPIC = "hpan-update-topic";
    private static final String HPAN_UPDATE_GROUP = "hpan-update-group";
    private static final String HPAN_UPDATE_OUTCOME_TOPIC = "hpanUpdateOutcome-topic";
    private static final String COMMANDS_TOPIC ="commands-topic";
    private static final String COMMANDS_GROUP ="commands-group";
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

                REWARD_RULE_TOPIC,
                REWARD_RULE_GROUP,
                BINDER_KAFKA_TYPE,
                BINDER_BROKER,

                TRANSACTIONS_TOPIC,
                TRANSACTIONS_GROUP,

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                TRANSACTION_REWARDED_TOPIC,

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                HPAN_UPDATE_TOPIC,
                HPAN_UPDATE_GROUP,

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                HPAN_UPDATE_OUTCOME_TOPIC,

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                COMMANDS_TOPIC,
                COMMANDS_GROUP,

                BINDER_KAFKA_TYPE,
                BINDER_BROKER,
                TRX_RESPONSE_TOPIC,
                TRX_RESPONSE_GROUP
        );
    }

    @Test
    void notifyRewardRuleBuilder() {

        errorNotifyMock(REWARD_RULE_TOPIC, REWARD_RULE_GROUP, true, true );

        boolean result = rewardErrorNotifierService.notifyRewardRuleBuilder(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        Assertions.assertTrue(result);
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyTransactionEvaluation() {

        errorNotifyMock(TRANSACTIONS_TOPIC, TRANSACTIONS_GROUP, true, true );

        boolean result = rewardErrorNotifierService.notifyTransactionEvaluation(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        Assertions.assertTrue(result);
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyRewardedTransaction() {

        errorNotifyMock(TRANSACTION_REWARDED_TOPIC, null, false, false );

        boolean result = rewardErrorNotifierService.notifyRewardedTransaction(dummyMessage, DUMMY_MESSAGE, false, new Throwable(DUMMY_MESSAGE));

        Assertions.assertTrue(result);
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyTransactionResponse() {

        errorNotifyMock(TRX_RESPONSE_TOPIC, TRX_RESPONSE_GROUP, true, true );

        boolean result = rewardErrorNotifierService.notifyTransactionResponse(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        Assertions.assertTrue(result);
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyRewardCommands() {

        errorNotifyMock(COMMANDS_TOPIC, COMMANDS_GROUP, true, true);

        rewardErrorNotifierService.notifyRewardCommands(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyHpanUpdateOutcome() {

        errorNotifyMock(HPAN_UPDATE_OUTCOME_TOPIC, null, true, false );

        boolean result = rewardErrorNotifierService.notifyHpanUpdateOutcome(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        Assertions.assertTrue(result);
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    private void errorNotifyMock(String topic, String group, boolean retryable, boolean resendApplication ) {
        Mockito.when(errorNotifierServiceMock.notify(eq(BINDER_KAFKA_TYPE), eq(BINDER_BROKER),
                eq(topic), eq(group), eq(dummyMessage), eq(DUMMY_MESSAGE), eq(retryable), eq(resendApplication), any()))
                .thenReturn(true);
    }
}