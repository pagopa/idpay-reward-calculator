package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RewardValueDTO implements AnyOfInitiativeRewardRule {
    @JsonProperty("_type")
    private FieldEnumRewardDTO _type;
    @JsonProperty("rewardValue")
    private Integer rewardValue;
}
