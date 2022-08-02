package it.gov.pagopa.reward.model.counters;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class InitiativeCounters extends Counters {
    @Id
    private String initiativeId;
    @Builder.Default
    private Map<String, Counters> dailyCounters = new HashMap<>();
    @Builder.Default
    private Map<String, Counters> weeklyCounters = new HashMap<>();
    @Builder.Default
    private Map<String, Counters> monthlyCounters = new HashMap<>();
    @Builder.Default
    private Map<String, Counters> yearlyCounters = new HashMap<>();
}
