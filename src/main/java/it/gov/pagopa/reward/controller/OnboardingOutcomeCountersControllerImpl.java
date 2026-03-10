package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.EvaluationDTO;
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
  public Mono<EvaluationDTO> processOnboardingOutcome(EvaluationDTO evaluationDTO) {
    log.info("[ONBOARDING_OUTCOME] Received onboarding outcome request for initiative {} and user {}",
        evaluationDTO.initiativeId(), evaluationDTO.userId());

    return onboardingOutcomeMediatorService.processOnboardingOutcome(evaluationDTO);
  }
}
