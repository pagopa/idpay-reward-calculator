package it.gov.pagopa.reward.controller;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@RequestMapping("/reward/onboarding")
public interface OnboardingOutcomeCountersController {

  @PutMapping("/{initiativeId}/{userId}/counters")
  Mono<Void> processOnboardingOutcome(@PathVariable("initiativeId") String initiativeId,
      @PathVariable("userId") String userId);
}
