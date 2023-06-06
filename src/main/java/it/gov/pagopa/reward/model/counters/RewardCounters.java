package it.gov.pagopa.reward.model.counters;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
public class RewardCounters extends Counters {
    private boolean exhaustedBudget;
    private BigDecimal initiativeBudget;
    private long version;

    private Map<String, Counters> dailyCounters;
    private Map<String, Counters> weeklyCounters;
    private Map<String, Counters> monthlyCounters;
    private Map<String, Counters> yearlyCounters;
}
