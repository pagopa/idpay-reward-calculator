package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class ThresholdDTO implements AnyOfInitiativeRewardRule {
    @JsonProperty("_type")
    private FieldEnumRewardDTO _type;
    @JsonProperty("thresholdType")
    private ThresholdTypeDTO thresholdType;
}
