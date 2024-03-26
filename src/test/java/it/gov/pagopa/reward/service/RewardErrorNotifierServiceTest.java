package it.gov.pagopa.reward.service;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import it.gov.pagopa.reward.model.SrcDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private ArgumentCaptor<SrcDetails> srcDetailsCaptor;

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
        srcDetailsCaptor = ArgumentCaptor.forClass(SrcDetails.class);
    }

    @Test
    void notifyRewardRuleBuilder() {

        errorNotifyMock(srcDetailsCaptor, REWARD_RULE_GROUP, true, true );

        boolean result = rewardErrorNotifierService.notifyRewardRuleBuilder(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        SrcDetails capturedSrcDetails = srcDetailsCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getSrcType());
        Assertions.assertEquals(REWARD_RULE_TOPIC, capturedSrcDetails.getSrcTopic());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getSrcServer());
        Assertions.assertTrue(result);

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyTransactionEvaluation() {

        errorNotifyMock(srcDetailsCaptor, TRANSACTIONS_GROUP, true, true );

        boolean result = rewardErrorNotifierService.notifyTransactionEvaluation(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        SrcDetails capturedSrcDetails = srcDetailsCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getSrcType());
        Assertions.assertEquals(TRANSACTIONS_TOPIC, capturedSrcDetails.getSrcTopic());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getSrcServer());
        Assertions.assertTrue(result);

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyRewardedTransaction() {

        errorNotifyMock( srcDetailsCaptor, null, false, false );

        boolean result = rewardErrorNotifierService.notifyRewardedTransaction(dummyMessage, DUMMY_MESSAGE, false, new Throwable(DUMMY_MESSAGE));

        SrcDetails capturedSrcDetails = srcDetailsCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getSrcType());
        Assertions.assertEquals(TRANSACTION_REWARDED_TOPIC, capturedSrcDetails.getSrcTopic());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getSrcServer());

        Assertions.assertTrue(result);

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyTransactionResponse() {

        errorNotifyMock(srcDetailsCaptor, TRX_RESPONSE_GROUP, true, true );


        boolean result = rewardErrorNotifierService.notifyTransactionResponse(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        SrcDetails capturedSrcDetails = srcDetailsCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getSrcType());
        Assertions.assertEquals(TRX_RESPONSE_TOPIC, capturedSrcDetails.getSrcTopic());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getSrcServer());
        Assertions.assertTrue(result);

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyRewardCommands() {

        errorNotifyMock( srcDetailsCaptor, COMMANDS_GROUP, true, true);

        rewardErrorNotifierService.notifyRewardCommands(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        SrcDetails capturedSrcDetails = srcDetailsCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getSrcType());
        Assertions.assertEquals(COMMANDS_TOPIC, capturedSrcDetails.getSrcTopic());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getSrcServer());



        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyHpanUpdateEvaluation() {

        errorNotifyMock(srcDetailsCaptor, HPAN_UPDATE_GROUP, true, true);

        boolean result = rewardErrorNotifierService.notifyHpanUpdateEvaluation(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        SrcDetails capturedSrcDetails = srcDetailsCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getSrcType());
        Assertions.assertEquals(HPAN_UPDATE_TOPIC, capturedSrcDetails.getSrcTopic());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getSrcServer());

        Assertions.assertTrue(result);

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyHpanUpdateOutcome() {

        errorNotifyMock(srcDetailsCaptor, null, true, false );

        boolean result = rewardErrorNotifierService.notifyHpanUpdateOutcome(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        SrcDetails capturedSrcDetails = srcDetailsCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getSrcType());
        Assertions.assertEquals(HPAN_UPDATE_OUTCOME_TOPIC, capturedSrcDetails.getSrcTopic());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getSrcServer());

        Assertions.assertTrue(result);
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    private void errorNotifyMock(ArgumentCaptor<SrcDetails> srcDetailsCaptor, String group, boolean retryable, boolean resendApplication ) {
        Mockito.when(errorNotifierServiceMock.notify(
                        srcDetailsCaptor.capture(),
                        eq(DUMMY_MESSAGE),
                        eq(retryable),
                        any(),
                        eq(group),
                        eq(resendApplication),
                        eq(dummyMessage)
                )
        ).thenReturn(true);
    }
}