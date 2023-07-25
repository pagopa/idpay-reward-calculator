package it.gov.pagopa.reward.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingRejectionReason implements Serializable {
    @NotNull
    private OnboardingRejectionReasonType type;
    @NotNull
    private String code;
    private String authority;
    private String authorityLabel;
    private String detail;

    public enum OnboardingRejectionReasonType {
        TECHNICAL_ERROR,
        CONSENSUS_MISSED,
        INVALID_REQUEST,
        BUDGET_EXHAUSTED,
        AUTOMATED_CRITERIA_FAIL,
        ISEE_TYPE_KO,
        FAMILY_KO,
        BIRTHDATE_KO,
        OUT_OF_RANKING
    }
}