package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.common.reactive.kafka.exception.UncommittableError;
import it.gov.pagopa.common.utils.MethodRetryUtils;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.service.ErrorNotifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
@Slf4j
public class RewardNotifierServiceImpl implements RewardNotifierService {

    private final StreamBridge streamBridge;
    private final ErrorNotifierService errorNotifierService;

    private final int rewardNotifyMaxRetries;

    public RewardNotifierServiceImpl(
            @Value("${app.trx-retries.reward-notify.retries}") int rewardNotifyMaxRetries,

            StreamBridge streamBridge, ErrorNotifierService errorNotifierService) {
        this.rewardNotifyMaxRetries = rewardNotifyMaxRetries;
        this.streamBridge = streamBridge;
        this.errorNotifierService = errorNotifierService;
    }

    /** Declared just to let know Spring to connect the producer at startup */
    @Configuration
    static class RewardNotifierPublisherConfig {
        @Bean
        public Supplier<Flux<Message<RewardTransactionDTO>>> trxProcessorOut() {
            return Flux::empty;
        }
    }

    @Override
    public boolean notify(RewardTransactionDTO reward) {
        return streamBridge.send("trxProcessorOut-out-0",
               buildMessage(reward));
    }

    public static Message<RewardTransactionDTO> buildMessage(RewardTransactionDTO reward){
        return MessageBuilder.withPayload(reward)
                .setHeader(KafkaHeaders.MESSAGE_KEY,reward.getUserId()).build();
    }

    @Override
    public void notifyFallbackToErrorTopic(RewardTransactionDTO r) {
        MethodRetryUtils.exec("RewardNotifier-FallbackToErrorTopic", () -> notifyFallbackToErrorTopicInner(r), rewardNotifyMaxRetries);
    }

    public void notifyFallbackToErrorTopicInner(RewardTransactionDTO r) {
        try {
            if (!notify(r)) {
                throw new IllegalStateException("[REWARD] Something gone wrong while reward notify");
            }
        } catch (Exception e) {
            log.error("[UNEXPECTED_TRX_PROCESSOR_ERROR] Unexpected error occurred publishing rewarded transaction: {}", r, e);
            try{
                if(!errorNotifierService.notifyRewardedTransaction(RewardNotifierServiceImpl.buildMessage(r), "[REWARD] An error occurred while publishing the transaction evaluation result", true, e)){
                    throw new IllegalStateException("[REWARD] Something gone wrong while reward notify into error topic");
                }
            } catch (Exception exWhenError){
                throw new UncommittableError("[UNEXPECTED_TRX_PROCESSOR_ERROR] Cannot publish result neither in error topic!", exWhenError);
            }
        }
    }
}
