package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FilterDTO implements AnyOfInitiativeRewardRule {
    @JsonProperty("_type")
    private FieldEnumRewardDTO _type;
    @JsonProperty("field")
    private String field;
    @JsonProperty("fiterOperator")
    private String filterOperator;
    @JsonProperty("value")
    private String value;
}
