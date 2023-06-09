package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;

public interface RewardNotifierService {
    boolean notify(RewardTransactionDTO reward);
    void notifyFallbackToErrorTopic(RewardTransactionDTO reward);
}
