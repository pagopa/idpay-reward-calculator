package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class ThresholdTypeDTO {
    @JsonProperty("_type")
    private FieldEnumThresholdDTO _type;
    @JsonProperty("from")
    private String from;
    @JsonProperty("fromIncluded")
    private boolean fromIncluded;

}
