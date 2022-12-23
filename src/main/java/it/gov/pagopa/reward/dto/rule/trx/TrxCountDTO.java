package it.gov.pagopa.reward.dto.rule.trx;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class TrxCountDTO implements InitiativeTrxCondition, InitiativeTrxConsequence {
    private Long from;
    private boolean fromIncluded;

    private Long to;
    private boolean toIncluded;
}
