package it.gov.pagopa.reward.dto.rule.trx;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DayOfWeekDTO extends ArrayList<DayOfWeekDTO.DayConfig> implements InitiativeTrxCondition {

    public DayOfWeekDTO(){
        super();
    }

    public DayOfWeekDTO(List<DayOfWeekDTO.DayConfig> list){
        super(list);
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DayConfig {
        private Set<DayOfWeek> daysOfWeek;
        private List<Interval> intervals;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Interval {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss[.SSS]")
        private LocalTime startTime;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss[.SSS]")
        private LocalTime endTime;
    }
}
