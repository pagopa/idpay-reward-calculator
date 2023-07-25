package it.gov.pagopa.reward.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BaseOnboardingInfo implements OnboardingInfo{
    private String initiativeId;
    private String familyId;
}
