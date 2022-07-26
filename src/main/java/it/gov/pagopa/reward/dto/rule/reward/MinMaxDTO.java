package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MinMaxDTO implements AnyOfInitiativeRewardRule {
    @JsonProperty("min_max")
    private String minMax;
}
