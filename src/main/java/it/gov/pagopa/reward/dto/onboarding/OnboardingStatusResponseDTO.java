package it.gov.pagopa.reward.dto.onboarding;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record OnboardingStatusResponseDTO(
    String status,
    Instant statusDate,
    Instant onboardingOkDate,
    @JsonProperty("family_id") @JsonAlias("familyId") String familyId
) {}
