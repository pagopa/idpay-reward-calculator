package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * AnyOfInitiativeRewardRule
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RewardGroupsDTO.class, name = "RewardGroupsDTO"),
        @JsonSubTypes.Type(value = RewardValueDTO.class, name = "RewardValueDTO"),
        @JsonSubTypes.Type(value = DayOfWeekDTO.class, name = "DayOfWeekDTO"),
        @JsonSubTypes.Type(value = ThresholdDTO.class, name = "ThresholdDTO"),
        @JsonSubTypes.Type(value = FilterDTO.class, name = "FilterDTO"),
        @JsonSubTypes.Type(value = RewardLimitDTO.class, name = "RewardLimitDTO"),
        @JsonSubTypes.Type(value = MinMaxDTO.class, name = "MinMaxDTO")
})
public interface AnyOfInitiativeRewardRule {
}
