package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class TrxNotifierServiceImpl implements TrxNotifierService {

    private final StreamBridge streamBridge;

    public TrxNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean notify(TransactionDTO trx) {
        return streamBridge.send("trxResubmitter-out-0",
               buildMessage(trx));
    }

    public static Message<TransactionDTO> buildMessage(TransactionDTO trx){
        return MessageBuilder.withPayload(trx)
                .setHeader(KafkaHeaders.MESSAGE_KEY,trx.getUserId()).build();
    }
}
