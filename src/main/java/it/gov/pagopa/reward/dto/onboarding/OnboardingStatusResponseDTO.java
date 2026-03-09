package it.gov.pagopa.reward.dto.onboarding;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record OnboardingStatusResponseDTO(
    String status,
    LocalDateTime statusDate,
    LocalDateTime onboardingOkDate,
    @JsonProperty("family_id") @JsonAlias("familyId") String familyId
) {}
