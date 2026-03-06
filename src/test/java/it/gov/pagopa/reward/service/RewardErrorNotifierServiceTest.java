package it.gov.pagopa.reward.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import it.gov.pagopa.reward.config.KafkaConfiguration;
import java.util.Map;
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

    private static final String COMMANDS_TOPIC ="commands-topic";
    private static final String COMMANDS_GROUP ="commands-group";
    private static final Message<String> dummyMessage = MessageBuilder.withPayload(DUMMY_MESSAGE).build();

    private final static String REWARD_RULE_CONSOMER_IN_0 = "rewardRuleConsumer-in-0";
    private final static String TRX_PROCESSOR_IN_0 = "trxProcessor-in-0";
    private final static String TRX_PROCESSOR_OUT_0 = "trxProcessorOut-out-0";
    private final static String COMMANDS_CONSUMER_IN_0 = "commandsConsumer-in-0";
    private final static String TRX_RESPONSE_CONSUMER_IN_0 = "trxResponseConsumer-in-0";

    @Mock
    private ErrorNotifierService errorNotifierServiceMock;
    private RewardErrorNotifierService rewardErrorNotifierService;
    private ArgumentCaptor<KafkaConfiguration.BaseKafkaInfoDTO> baseKafkaInfoDTOArgumentCaptor;
    @Mock
    private KafkaConfiguration kafkaConfigurationMock;

    @BeforeEach
    void setUp() {
        rewardErrorNotifierService = new RewardErrorNotifierServiceImpl(
                errorNotifierServiceMock,
                kafkaConfigurationMock
        );
        baseKafkaInfoDTOArgumentCaptor = ArgumentCaptor.forClass(KafkaConfiguration.BaseKafkaInfoDTO.class);

    }

    @Test
    void notifyRewardRuleBuilder() {

        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(kafkaConfigurationMock.getStream().getBindings()).thenReturn(
                Map.of(
                        REWARD_RULE_CONSOMER_IN_0,
                        KafkaConfiguration.KafkaInfoDTO.builder()
                                .group(REWARD_RULE_GROUP)
                                .type(BINDER_KAFKA_TYPE)
                                .brokers(BINDER_BROKER)
                                .destination(REWARD_RULE_TOPIC)
                                .build()
                )
        );

        errorNotifyMock(baseKafkaInfoDTOArgumentCaptor, true, true );

        boolean result = rewardErrorNotifierService.notifyRewardRuleBuilder(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        KafkaConfiguration.BaseKafkaInfoDTO capturedSrcDetails = baseKafkaInfoDTOArgumentCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getType());
        Assertions.assertEquals(REWARD_RULE_TOPIC, capturedSrcDetails.getDestination());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getBrokers());
        Assertions.assertEquals(REWARD_RULE_GROUP, capturedSrcDetails.getGroup());
        Assertions.assertTrue(result);

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyTransactionEvaluation() {

        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(kafkaConfigurationMock.getStream().getBindings()).thenReturn(
                Map.of(
                        TRX_PROCESSOR_IN_0,
                        KafkaConfiguration.KafkaInfoDTO.builder()
                                .group(TRANSACTIONS_GROUP)
                                .type(BINDER_KAFKA_TYPE)
                                .brokers(BINDER_BROKER)
                                .destination(TRANSACTIONS_TOPIC)
                                .build()
                )
        );
        errorNotifyMock(baseKafkaInfoDTOArgumentCaptor, true, true );

        boolean result = rewardErrorNotifierService.notifyTransactionEvaluation(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        KafkaConfiguration.BaseKafkaInfoDTO capturedSrcDetails = baseKafkaInfoDTOArgumentCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getType());
        Assertions.assertEquals(TRANSACTIONS_TOPIC, capturedSrcDetails.getDestination());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getBrokers());
        Assertions.assertEquals(TRANSACTIONS_GROUP, capturedSrcDetails.getGroup());
        Assertions.assertTrue(result);

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyRewardedTransaction() {

        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(kafkaConfigurationMock.getStream().getBindings()).thenReturn(
                Map.of(
                        TRX_PROCESSOR_OUT_0,
                        KafkaConfiguration.KafkaInfoDTO.builder()
                                .group(null)
                                .type(BINDER_KAFKA_TYPE)
                                .brokers(BINDER_BROKER)
                                .destination(TRANSACTION_REWARDED_TOPIC)
                                .build()
                )
        );
        errorNotifyMock( baseKafkaInfoDTOArgumentCaptor, false, false );

        boolean result = rewardErrorNotifierService.notifyRewardedTransaction(dummyMessage, DUMMY_MESSAGE, false, new Throwable(DUMMY_MESSAGE));

        KafkaConfiguration.BaseKafkaInfoDTO capturedSrcDetails = baseKafkaInfoDTOArgumentCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getType());
        Assertions.assertEquals(TRANSACTION_REWARDED_TOPIC, capturedSrcDetails.getDestination());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getBrokers());
        Assertions.assertNull(capturedSrcDetails.getGroup());
        Assertions.assertTrue(result);

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyTransactionResponse() {

        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(kafkaConfigurationMock.getStream().getBindings()).thenReturn(
                Map.of(
                        TRX_RESPONSE_CONSUMER_IN_0,
                        KafkaConfiguration.KafkaInfoDTO.builder()
                                .group(TRX_RESPONSE_GROUP)
                                .type(BINDER_KAFKA_TYPE)
                                .brokers(BINDER_BROKER)
                                .destination(TRX_RESPONSE_TOPIC)
                                .build()
                )
        );

        errorNotifyMock(baseKafkaInfoDTOArgumentCaptor, true, true );

        boolean result = rewardErrorNotifierService.notifyTransactionResponse(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        KafkaConfiguration.BaseKafkaInfoDTO capturedSrcDetails = baseKafkaInfoDTOArgumentCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getType());
        Assertions.assertEquals(TRX_RESPONSE_TOPIC, capturedSrcDetails.getDestination());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getBrokers());
        Assertions.assertEquals(TRX_RESPONSE_GROUP, capturedSrcDetails.getGroup());
        Assertions.assertTrue(result);

        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }
    @Test
    void notifyRewardCommands() {
        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(kafkaConfigurationMock.getStream().getBindings()).thenReturn(
                Map.of(
                        COMMANDS_CONSUMER_IN_0,
                        KafkaConfiguration.KafkaInfoDTO.builder()
                                .group(COMMANDS_GROUP)
                                .type(BINDER_KAFKA_TYPE)
                                .brokers(BINDER_BROKER)
                                .destination(COMMANDS_TOPIC)
                                .build()
                )
        );

        errorNotifyMock( baseKafkaInfoDTOArgumentCaptor, true, true);

        rewardErrorNotifierService.notifyRewardCommands(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));

        KafkaConfiguration.BaseKafkaInfoDTO capturedSrcDetails = baseKafkaInfoDTOArgumentCaptor.getValue();

        Assertions.assertEquals(BINDER_KAFKA_TYPE, capturedSrcDetails.getType());
        Assertions.assertEquals(COMMANDS_TOPIC, capturedSrcDetails.getDestination());
        Assertions.assertEquals(BINDER_BROKER, capturedSrcDetails.getBrokers());
        Assertions.assertEquals(COMMANDS_GROUP, capturedSrcDetails.getGroup());


        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock);
    }

    private void errorNotifyMock(ArgumentCaptor<KafkaConfiguration.BaseKafkaInfoDTO> baseKafkaInfoDTO, boolean retryable, boolean resendApplication ) {
        Mockito.when(errorNotifierServiceMock.notify(
                        baseKafkaInfoDTO.capture(),
                        eq(DUMMY_MESSAGE),
                        eq(retryable),
                        any(),
                        eq(resendApplication),
                        eq(dummyMessage)
                )
        ).thenReturn(true);
    }
}