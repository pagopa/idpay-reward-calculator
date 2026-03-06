package it.gov.pagopa.reward.dto;

import it.gov.pagopa.reward.model.OnboardingRejectionReason;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event payload used to evaluate onboarding results.
 */
public record EvaluationDTO(
        @NotEmpty String userId,
        String familyId,
        @NotEmpty String initiativeId,
        String initiativeName,
        LocalDate initiativeEndDate,
        String organizationId,
        @NotEmpty String status,
        @NotNull LocalDateTime admissibilityCheckDate,
        LocalDateTime criteriaConsensusTimestamp,
        @NotNull List<OnboardingRejectionReason> onboardingRejectionReasons,
        Long beneficiaryBudgetCents,
        String initiativeRewardType,
        String organizationName,
        Boolean isLogoPresent,
        Long maxTrx,
        String serviceId,
        String channel,
        String userMail,
        String name,
        String surname
) {
}
