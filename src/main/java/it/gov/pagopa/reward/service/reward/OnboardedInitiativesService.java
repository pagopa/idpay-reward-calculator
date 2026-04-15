package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.model.OnboardingInfo;
import java.time.Instant;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

/**
 * This component will retrieve initiatives to which the input userId has been onboarded
 * */
public interface OnboardedInitiativesService {
    Mono<Pair<InitiativeConfig, OnboardingInfo>> isOnboarded(String userId, Instant trxDate, String initiativeId);
}
