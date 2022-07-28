package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RewardGroupsDTO.class, name = "rewardGroups"),
        @JsonSubTypes.Type(value = RewardValueDTO.class, name = "rewardValue"),
})
public interface InitiativeRewardRule extends InitiativeTrxConsequence {
}
