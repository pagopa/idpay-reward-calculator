package it.gov.pagopa.reward.model.counters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
    private Map<String, Counters> dailyCounters = new HashMap<>();
    private Map<String, Counters> weeklyCounters = new HashMap<>();
    private Map<String, Counters> monthlyCounters = new HashMap<>();
    private Map<String, Counters> yearlyCounters = new HashMap<>();
}
