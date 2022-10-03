package it.gov.pagopa.reward.dto.rule.trx;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class TrxCountDTO implements InitiativeTrxCondition {
    private Long from;
    private boolean fromIncluded;

    private Long to;
    private boolean toIncluded;
}
