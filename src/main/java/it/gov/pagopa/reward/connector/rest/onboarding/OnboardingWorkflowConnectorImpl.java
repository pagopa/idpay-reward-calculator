package it.gov.pagopa.reward.connector.rest.onboarding;

import it.gov.pagopa.reward.dto.onboarding.OnboardingStatusResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class OnboardingWorkflowConnectorImpl implements OnboardingWorkflowConnector {

    private final String onboardingStatusPath;
    private final WebClient webClient;

    public OnboardingWorkflowConnectorImpl(
            WebClient.Builder webClientBuilder,
            @Value("${rest-client.onboarding-workflow.base-url}") String baseUrl,
            @Value("${rest-client.onboarding-workflow.get-status-path}") String getStatusPath) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        onboardingStatusPath = getStatusPath;
    }

    @Override
    public Mono<OnboardingStatusResponseDTO> getOnboardingStatus(String userId, String initiativeId) {
        log.debug("[ONBOARDING_WORKFLOW][GET_STATUS] Fetching onboarding status for userId: {}, initiativeId: {}",
                userId, initiativeId);
        return webClient.get()
                .uri(onboardingStatusPath, initiativeId, userId)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                        response -> Mono.empty())
                .bodyToMono(OnboardingStatusResponseDTO.class)
                .doOnNext(r -> log.info(
                        "[ONBOARDING_WORKFLOW][GET_STATUS] Status for userId: {}, initiativeId: {} -> {}, familyIdPresent: {}, familyIdMasked: {}",
                        userId, initiativeId, r.status(), r.familyId() != null && !r.familyId().isBlank(), maskFamilyId(r.familyId())))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("[ONBOARDING_WORKFLOW][GET_STATUS] HTTP error {} fetching status for userId: {}, initiativeId: {}",
                            ex.getStatusCode(), userId, initiativeId);
                    return Mono.empty();
                })
                .onErrorResume(ex -> {
                    log.error("[ONBOARDING_WORKFLOW][GET_STATUS] Error fetching status for userId: {}, initiativeId: {}",
                            userId, initiativeId);
                    return Mono.empty();
                });
    }

        private String maskFamilyId(String familyId) {
                if (familyId == null || familyId.isBlank()) {
                        return "<null>";
                }
                if (familyId.length() <= 4) {
                        return "****";
                }
                return "%s****%s".formatted(familyId.substring(0, 2), familyId.substring(familyId.length() - 2));
        }
}
