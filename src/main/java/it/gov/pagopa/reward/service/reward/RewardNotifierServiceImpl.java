package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
public class RewardNotifierServiceImpl implements RewardNotifierService {

    private final StreamBridge streamBridge;

    public RewardNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean notify(RewardTransactionDTO reward) {
        return streamBridge.send("trxProcessor-out-0", reward);
    }
}
