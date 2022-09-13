package it.gov.pagopa.reward.dto.trx;

import it.gov.pagopa.reward.model.TransactionProcessed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefundInfo {
    private List<TransactionProcessed> previousTrxs;
    private Map<String, BigDecimal> previousRewards;
}
