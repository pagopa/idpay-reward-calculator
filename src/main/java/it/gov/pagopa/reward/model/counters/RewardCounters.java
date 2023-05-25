package it.gov.pagopa.reward.model.counters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class RewardCounters extends Counters {
    private boolean exhaustedBudget;
    private BigDecimal initiativeBudget;
    private long version;

    private Map<String, Counters> dailyCounters;
    private Map<String, Counters> weeklyCounters;
    private Map<String, Counters> monthlyCounters;
    private Map<String, Counters> yearlyCounters;
}
