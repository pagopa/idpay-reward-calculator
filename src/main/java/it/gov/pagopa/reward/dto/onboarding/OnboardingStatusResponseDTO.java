package it.gov.pagopa.reward.dto.onboarding;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Response DTO for the onboarding status returned by the idpay-onboarding-workflow service.
 *
 * @param status   onboarding status (e.g. ONBOARDING_OK, ON_EVALUATION, ONBOARDING_KO ...)
 * @param familyId family group identifier, present only for NF (Nucleo Familiare) initiatives
 */
public record OnboardingStatusResponseDTO(String status, @JsonAlias("family_id") String familyId) {
}
