package it.gov.pagopa.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InitiativeConfig {

    private String initiativeId;
    private BigDecimal budget;
    private LocalDate endDate;
    private boolean dailyThreshold;
    private boolean weeklyThreshold;
    private boolean monthlyThreshold;
    private boolean yearlyThreshold;

}
