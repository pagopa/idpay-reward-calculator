package it.gov.pagopa.reward.model.counters;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class Counters {
    @Builder.Default
    private Long trxNumber = 0L;
    @Builder.Default
    private BigDecimal totalReward = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
}
