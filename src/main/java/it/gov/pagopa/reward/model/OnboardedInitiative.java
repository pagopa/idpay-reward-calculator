package it.gov.pagopa.reward.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OnboardedInitiative {
    private String initiativeId;
    private LocalDate acceptanceDate;
    private String status;
    private List<ActiveTimeInterval> activeTimeIntervals;
}
