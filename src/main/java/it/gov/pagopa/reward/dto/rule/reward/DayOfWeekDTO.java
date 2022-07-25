package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class DayOfWeekDTO implements AnyOfInitiativeRewardRule {
    @JsonProperty("_type")
    private FieldEnumRewardDTO _type;

    @JsonProperty("daysAllowed")
    private List<DaysAllowedDTO> daysAllowed;
}
