package it.gov.pagopa.reward.dto.mapper.trx;

import it.gov.pagopa.reward.dto.trx.LastTrxInfoDTO;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class BaseTransactionProcessed2LastTrxInfoDTOMapper implements Function<BaseTransactionProcessed, LastTrxInfoDTO> {
    @Override
    public LastTrxInfoDTO apply(BaseTransactionProcessed baseTransactionProcessed) {
        LastTrxInfoDTO lastTrx = new LastTrxInfoDTO();
        lastTrx.setTrxId(baseTransactionProcessed.getId());
        lastTrx.setOperationTypeTranscoded(baseTransactionProcessed.getOperationTypeTranscoded());
        lastTrx.setElaborationDateTime(baseTransactionProcessed.getElaborationDateTime());
        lastTrx.setAccruedReward(getAccruedRewards(baseTransactionProcessed));
        return lastTrx;
    }

    private Map<String, Long> getAccruedRewards(BaseTransactionProcessed baseTransactionProcessed) {
        Map<String, Long> accruedRewards = new HashMap<>();
        baseTransactionProcessed.getRewards()
                .forEach((k,v) -> accruedRewards.put(k, v.getAccruedRewardCents()));
        return accruedRewards;
    }
}
