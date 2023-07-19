package it.gov.pagopa.reward.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Document(collection = "onboarding_families")
public class OnboardingFamilies {
    @Id
    private String id;
    private String familyId;
    private String initiativeId;
    private Set<String> memberIds;
    private OnboardingFamilyEvaluationStatus status;
    private List<OnboardingRejectionReason> onboardingRejectionReasons;
    private LocalDateTime createDate;


    public static String buildId(String familyId, String initiativeId) {
        return "%s_%s".formatted(familyId, initiativeId);
    }

    public enum OnboardingFamilyEvaluationStatus {
        /** if family members are allowed to join */
        ONBOARDING_KO,
        /** if family members are NOT allowed to join */
        ONBOARDING_OK,
        /** if family evaluation is in progress */
        IN_PROGRESS,
    }

}
