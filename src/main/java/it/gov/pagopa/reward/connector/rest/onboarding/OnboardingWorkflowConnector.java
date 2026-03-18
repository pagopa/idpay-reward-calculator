package it.gov.pagopa.reward.connector.rest.onboarding;

import it.gov.pagopa.reward.dto.onboarding.OnboardingStatusResponseDTO;
import reactor.core.publisher.Mono;

/**
 * Connector to the idpay-onboarding-workflow microservice.
 * Retrieves the onboarding status for a (userId, initiativeId) pair.
 */
public interface OnboardingWorkflowConnector {

    /**
     * Returns the onboarding status for the given user and initiative.
     * Emits {@code Mono.empty()} when the service returns a non-2xx response or an error occurs.
     */
    Mono<OnboardingStatusResponseDTO> getOnboardingStatus(String userId, String initiativeId);
}
