package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FieldEnumRewardDTO {
    REWARD_GROUPS("rewardGroups"),
    REWARD_VALUE("rewardValue"),
    DAY_OF_WEEK("dayOfWeek"),
    THRESHOLD("threshold"),
    MCC_FILTER("mccFilter"),
    FILTER("filter"),
    REWARD_LIMIT("rewardLimit");

    private String value;
    FieldEnumRewardDTO(String value){
        this.value=value;
    }
    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static FieldEnumRewardDTO fromValue(String text) {
        for (FieldEnumRewardDTO b : FieldEnumRewardDTO.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
