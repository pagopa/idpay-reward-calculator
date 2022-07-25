package it.gov.pagopa.reward.model.counters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Counters {
    private String trxNumber;
    private Long totalReward;
    private Long totalAmount;
}
