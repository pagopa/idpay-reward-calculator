package it.gov.pagopa.reward.model;

import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldNameConstants
public class OnboardedInitiative {
    private String initiativeId;
    private LocalDateTime acceptanceDate;
    private HpanInitiativeStatus status;
    private LocalDateTime lastEndInterval;
    private List<ActiveTimeInterval> activeTimeIntervals;
}
