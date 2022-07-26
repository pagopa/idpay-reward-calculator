package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class DaysAllowedDTO {
    @JsonProperty("dayOfWeek")
    private String dayOfWeek;
    @JsonProperty("intervals")
    private IntervalDTO interval;
}
