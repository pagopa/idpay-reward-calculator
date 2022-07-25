package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class IntervalDTO {
    @JsonProperty("startTime")
    private LocalDate startTime;
    @JsonProperty("endTime")
    private LocalDate endTime;
}
