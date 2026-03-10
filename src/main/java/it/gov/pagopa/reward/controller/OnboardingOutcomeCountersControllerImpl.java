package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.service.lookup.OnboardingOutcomeMediatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequiredArgsConstructor
public class OnboardingOutcomeCountersControllerImpl implements OnboardingOutcomeCountersController {

  private final OnboardingOutcomeMediatorService onboardingOutcomeMediatorService;

  @Override
  public Mono<Void> processOnboardingOutcome(String initiativeId, String userId) {
    log.info("[ONBOARDING_OUTCOME] Received onboarding outcome request for initiative {} and user {}",
        initiativeId, userId);

    return onboardingOutcomeMediatorService.processOnboardingOutcome(initiativeId, userId);
  }
}
