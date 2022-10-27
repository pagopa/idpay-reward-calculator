package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.HpanUpdateOutcomeDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class HpanUpdateNotifierServiceImpl implements HpanUpdateNotifierService {
    private final StreamBridge streamBridge;

    public HpanUpdateNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean notify(HpanUpdateOutcomeDTO hpanUpdateOutcomeDTO) {
        return streamBridge.send("hpanUpdateOutcome-out-0",
                buildMessage(hpanUpdateOutcomeDTO));
    }
    public static Message<HpanUpdateOutcomeDTO> buildMessage(HpanUpdateOutcomeDTO hpanUpdateOutcomeDTO){
        return MessageBuilder.withPayload(hpanUpdateOutcomeDTO).build();
    }
}