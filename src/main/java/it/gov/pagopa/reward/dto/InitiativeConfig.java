package it.gov.pagopa.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InitiativeConfig {

    private String initiativeId;
    private boolean hasDailyThreshold;
    private boolean hasWeeklyThreshold;
    private boolean hasMonthlyThreshold;
    private boolean hasYearlyThreshold;

}
