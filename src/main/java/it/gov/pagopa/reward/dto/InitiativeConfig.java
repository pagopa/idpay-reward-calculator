package it.gov.pagopa.reward.dto;

import lombok.Data;

@Data
public class InitiativeConfig {

    private String initiativeId;
    private String hasDailyThreshold;
    private String hasMonthlyThreshold;
    private String hasYearlyThreshold;

}
