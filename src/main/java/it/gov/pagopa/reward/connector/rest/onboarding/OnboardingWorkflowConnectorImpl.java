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

    static final String ONBOARDING_STATUS_PATH = "/idpay/onboarding/{initiativeId}/{userId}/status";

    private final WebClient webClient;

    public OnboardingWorkflowConnectorImpl(
            WebClient.Builder webClientBuilder,
            @Value("${rest-client.onboarding-workflow.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<OnboardingStatusResponseDTO> getOnboardingStatus(String userId, String initiativeId) {
        log.debug("[ONBOARDING_WORKFLOW][GET_STATUS] Fetching onboarding status for userId: {}, initiativeId: {}",
                userId, initiativeId);
        return webClient.get()
                .uri(ONBOARDING_STATUS_PATH, initiativeId, userId)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                        response -> Mono.empty())
                .bodyToMono(OnboardingStatusResponseDTO.class)
                .doOnNext(r -> log.debug(
                        "[ONBOARDING_WORKFLOW][GET_STATUS] Status for userId: {}, initiativeId: {} -> {}",
                        userId, initiativeId, r.status()))
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
}
