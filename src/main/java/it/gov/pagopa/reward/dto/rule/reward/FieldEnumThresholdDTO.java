package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FieldEnumThresholdDTO {
    GREATER("greater"),
    SMALLER("smaller");

    private String value;
    FieldEnumThresholdDTO(String value){
        this.value = value;
    }
    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static FieldEnumThresholdDTO fromValue(String text) {
        for (FieldEnumThresholdDTO b : FieldEnumThresholdDTO.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
