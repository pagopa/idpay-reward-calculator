package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class TrxRePublisherServiceImpl implements TrxRePublisherService {

    private final StreamBridge streamBridge;

    public TrxRePublisherServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean notify(TransactionDTO trx) {
        clearStatus(trx);
        return streamBridge.send("trxResubmitter-out-0",
               buildMessage(trx));
    }

    private void clearStatus(TransactionDTO trx) {
        trx.setEffectiveAmount(null);
        trx.setRejectionReasons(new ArrayList<>());
        trx.setRefundInfo(null);
    }

    public static Message<TransactionDTO> buildMessage(TransactionDTO trx){
        return MessageBuilder.withPayload(trx)
                .setHeader(KafkaHeaders.MESSAGE_KEY,trx.getUserId()).build();
    }
}
