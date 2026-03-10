package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.web.exception.ErrorManager;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.reward.config.ServiceExceptionConfig;
import it.gov.pagopa.reward.dto.EvaluationDTO;
import it.gov.pagopa.reward.service.lookup.OnboardingOutcomeMediatorService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@WebFluxTest(controllers = {OnboardingOutcomeCountersController.class})
@Import({ServiceExceptionConfig.class, ErrorManager.class, ValidationExceptionHandler.class})
class OnboardingOutcomeCountersControllerImplTest {

  private static final String ONBOARDING_OUTCOME_PATH = "/reward/onboarding/outcome";

  @MockBean
  private OnboardingOutcomeMediatorService onboardingOutcomeMediatorService;

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void shouldProcessOnboardingOutcome() {
    EvaluationDTO payload = buildPayload();
    Mockito.when(onboardingOutcomeMediatorService.processOnboardingOutcome(payload))
        .thenReturn(Mono.just(payload));

    webTestClient.post()
        .uri(ONBOARDING_OUTCOME_PATH)
        .bodyValue(payload)
        .exchange()
        .expectStatus().isOk()
        .expectBody(EvaluationDTO.class).isEqualTo(payload);

    Mockito.verify(onboardingOutcomeMediatorService, Mockito.only())
        .processOnboardingOutcome(payload);
  }

  @Test
  void shouldReturnServerErrorWhenServiceFails() {
    EvaluationDTO payload = buildPayload();
    Mockito.when(onboardingOutcomeMediatorService.processOnboardingOutcome(payload))
        .thenReturn(Mono.error(new RuntimeException("boom")));

    webTestClient.post()
        .uri(ONBOARDING_OUTCOME_PATH)
        .bodyValue(payload)
        .exchange()
        .expectStatus().is5xxServerError();

    Mockito.verify(onboardingOutcomeMediatorService, Mockito.only())
        .processOnboardingOutcome(payload);
  }

  private EvaluationDTO buildPayload() {
    return new EvaluationDTO(
        "USER_ID",
        null,
        "INITIATIVE_ID",
        "Initiative name",
        LocalDate.now().plusDays(1),
        "ORG_ID",
        "ONBOARDING_OK",
        LocalDateTime.now(),
        null,
        List.of(),
        1_000L,
        "REWARD",
        "Org name",
        true,
        10L,
        "service-id",
        "CHANNEL",
        "mail@test.it",
        "name",
        "surname");
  }
}
