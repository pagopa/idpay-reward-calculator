package it.gov.pagopa.reward.model.counters;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InitiativeCounters extends Counters {
    @Id
    private String initiativeId;
    private Map<String, Counters> dailyCounters;
    private Map<String, Counters> monthlyCounters;
    private Map<String, Counters> yearlyCounters;
}
