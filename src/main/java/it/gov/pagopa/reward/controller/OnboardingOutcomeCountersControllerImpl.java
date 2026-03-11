package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.service.lookup.OnboardingOutcomeMediatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static it.gov.pagopa.reward.utils.Utils.sanitizeString;

@RestController
@Slf4j
@RequiredArgsConstructor
public class OnboardingOutcomeCountersControllerImpl implements OnboardingOutcomeCountersController {

  private final OnboardingOutcomeMediatorService onboardingOutcomeMediatorService;

  @Override
  public Mono<Void> processOnboardingOutcome(String initiativeId, String userId) {
    final String sanitizedInitiativeId = initiativeId == null ? null : sanitizeString(initiativeId);
    final String sanitizedUserId = userId == null ? null : sanitizeString(userId);

    log.info("[ONBOARDING_OUTCOME] Received onboarding outcome request for initiative {} and user {}",
        sanitizedInitiativeId, sanitizedUserId);

    return onboardingOutcomeMediatorService.processOnboardingOutcome(sanitizedInitiativeId, sanitizedUserId);
  }
}
