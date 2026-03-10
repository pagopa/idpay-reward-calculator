package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.EvaluationDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@RequestMapping("/reward/onboarding")
public interface OnboardingOutcomeCountersController {

  @PostMapping("/outcome")
  Mono<EvaluationDTO> processOnboardingOutcome(@Valid @RequestBody EvaluationDTO evaluationDTO);
}
