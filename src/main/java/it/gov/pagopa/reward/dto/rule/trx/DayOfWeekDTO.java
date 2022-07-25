package it.gov.pagopa.reward.dto.rule.trx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class DayOfWeekDTO implements InitiativeTrxCondition {
    private Set<DayOfWeek> dayOfWeeks;
    private IntervalDTO interval;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    @Builder
    public static class IntervalDTO {
        @JsonProperty("startTime")
        private LocalTime startTime;
        @JsonProperty("endTime")
        private LocalTime endTime;
    }
}
