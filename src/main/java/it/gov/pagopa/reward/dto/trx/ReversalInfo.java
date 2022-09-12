package it.gov.pagopa.reward.dto.trx;

import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.model.TransactionProcessed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReversalInfo {
    private List<TransactionProcessed> previousTrxs;
    private Map<String, Reward> previousRewards;
}
