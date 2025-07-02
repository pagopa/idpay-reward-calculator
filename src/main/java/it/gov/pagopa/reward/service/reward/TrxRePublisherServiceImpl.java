package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.function.Supplier;

@Service
public class TrxRePublisherServiceImpl implements TrxRePublisherService {
    private final StreamBridge streamBridge;
    public TrxRePublisherServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    /** Declared just to let know Spring to connect the producer at startup */
    @Configuration
    static class TrxRePublisherProducerConfig {
        @Bean
        public Supplier<Flux<Message<TransactionDTO>>> trxResubmitter() {
            return Flux::empty;
        }
    }

    @Override
    public boolean notify(TransactionDTO trx) {
        clearStatus(trx);
        return streamBridge.send("trxResubmitter-out-0",
               buildMessage(trx));
    }

    private void clearStatus(TransactionDTO trx) {
        trx.setEffectiveAmountCents(null);
        trx.setRejectionReasons(new ArrayList<>());
        trx.setRefundInfo(null);
    }

    public static Message<TransactionDTO> buildMessage(TransactionDTO trx){
        return MessageBuilder.withPayload(trx)
                .setHeader(KafkaHeaders.KEY,trx.getUserId()).build();
    }
}
