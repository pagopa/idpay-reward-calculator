package it.gov.pagopa.reward.dto.rule.trx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ThresholdDTO implements InitiativeTrxCondition {
    private BigDecimal from;
    private boolean fromIncluded;

    private BigDecimal to;
    private boolean toIncluded;
}
