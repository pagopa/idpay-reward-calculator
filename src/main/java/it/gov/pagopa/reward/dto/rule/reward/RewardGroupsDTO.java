package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class RewardGroupsDTO implements AnyOfInitiativeRewardRule{
    @JsonProperty("_type")
    private FieldEnumRewardDTO _type;
    @JsonProperty("rewardGroups")
    private List<RewardGroupDTO> rewardGroupDTOS;
}
