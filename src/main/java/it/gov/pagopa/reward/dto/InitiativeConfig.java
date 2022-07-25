package it.gov.pagopa.reward.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InitiativeConfig {

    private String initiativeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> automatedCriteriaCodes;
}
