package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import lombok.extern.slf4j.Slf4j;
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

    public RewardNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
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
                .setHeader(KafkaHeaders.KEY,reward.getUserId()).build();
    }
}
