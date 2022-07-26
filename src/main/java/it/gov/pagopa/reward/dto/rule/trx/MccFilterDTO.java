package it.gov.pagopa.reward.dto.rule.trx;

import lombok.*;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class MccFilterDTO implements InitiativeTrxCondition {
    private boolean allowedList;
    private Set<String> values;
}
