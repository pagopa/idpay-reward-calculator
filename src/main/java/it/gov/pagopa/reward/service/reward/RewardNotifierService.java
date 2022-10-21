package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;

public interface RewardNotifierService {
    boolean notify(RewardTransactionDTO reward);
}
