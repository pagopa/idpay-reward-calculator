package it.gov.pagopa.reward.event.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.messaging.DirectWithAttributesChannel;
import org.springframework.kafka.KafkaException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ChannelInterceptor;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

class TransactionProcessorPublishTimeoutTest extends BaseTransactionProcessorTest {

    @Autowired
    @Qualifier("trxProcessor-in-0")
    private DirectWithAttributesChannel trxProcessorInChannel;
    @Autowired
    @Qualifier("trxProcessor-out-0")
    private DirectWithAttributesChannel trxProcessorOutChannel;

    @Autowired
    private Transaction2RewardTransactionMapper rewardTransactionMapper;

    @Test
    void test() throws JsonProcessingException {
        final Logger errorNotifierLogger = (Logger) LoggerFactory.getLogger(ErrorNotifierServiceImpl.class.getName());
        Level errorNotifierLoggerLevel = errorNotifierLogger.getLevel();
        errorNotifierLogger.setLevel(Level.OFF);

        final ChannelInterceptor kafkaExceptionSimulator = new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                if(new String((byte[])message.getPayload()).contains("USERID0")){
                    throw new MessageHandlingException(message, new KafkaException(topicRewardProcessorOutcome));
                } else {
                    return message;
                }
            }
        };
        trxProcessorOutChannel.addInterceptor(0, kafkaExceptionSimulator);

        try {
            IntStream.range(0, 5).forEach(i -> publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, null, TransactionDTOFaker.mockInstance(i)));
            Awaitility.await().atMost(1, TimeUnit.SECONDS).await();
            IntStream.range(5, 7).forEach(i -> publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, null, TransactionDTOFaker.mockInstance(i)));

            consumeMessages(topicRewardProcessorOutcome, 6, 10000);

            final ConsumerRecord<String, String> errorMessage = consumeMessages(topicErrors, 1, 1000).get(0);

            final RewardTransactionDTO expectedErrorMessagePayload = rewardTransactionMapper.apply(TransactionDTOFaker.mockInstance(0));
            expectedErrorMessagePayload.setOperationTypeTranscoded(OperationType.CHARGE);
            expectedErrorMessagePayload.setEffectiveAmount(expectedErrorMessagePayload.getAmount());
            expectedErrorMessagePayload.setTrxChargeDate(expectedErrorMessagePayload.getTrxDate());
            expectedErrorMessagePayload.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE));
            expectedErrorMessagePayload.setStatus(RewardConstants.REWARD_STATE_REJECTED);

            checkErrorMessageHeaders(topicRewardProcessorOutcome, errorMessage, "An error occurred while publishing the transaction evaluation result", objectMapper.writeValueAsString(expectedErrorMessagePayload), false);

            Assertions.assertEquals(1, trxProcessorInChannel.getSubscriberCount());
        } finally {
            errorNotifierLogger.setLevel(errorNotifierLoggerLevel);
            trxProcessorOutChannel.removeInterceptor(kafkaExceptionSimulator);
        }
    }
}
