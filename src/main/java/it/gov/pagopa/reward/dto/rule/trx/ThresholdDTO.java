package it.gov.pagopa.reward.dto.rule.trx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ThresholdDTO implements InitiativeTrxCondition {
    private Long fromCents;
    private boolean fromIncluded;

    private Long toCents;
    private boolean toIncluded;
}
