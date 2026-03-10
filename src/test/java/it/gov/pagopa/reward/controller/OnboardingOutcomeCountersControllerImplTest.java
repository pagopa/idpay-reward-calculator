package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.web.exception.ErrorManager;
import it.gov.pagopa.common.web.exception.ValidationExceptionHandler;
import it.gov.pagopa.reward.config.ServiceExceptionConfig;
import it.gov.pagopa.reward.service.lookup.OnboardingOutcomeMediatorService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {OnboardingOutcomeCountersController.class})
@Import({ServiceExceptionConfig.class, ErrorManager.class, ValidationExceptionHandler.class})
class OnboardingOutcomeCountersControllerImplTest {

    private static final String ONBOARDING_OUTCOME_PATH = "/reward/onboarding/{initiativeId}/users/{userId}/counters";
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    private static final String USER_ID = "USER_ID";

  @MockBean
  private OnboardingOutcomeMediatorService onboardingOutcomeMediatorService;

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void shouldProcessOnboardingOutcome() {
    Mockito.when(onboardingOutcomeMediatorService.processOnboardingOutcome(INITIATIVE_ID, USER_ID))
        .thenReturn(Mono.empty());

    webTestClient.post()
        .uri(ONBOARDING_OUTCOME_PATH, INITIATIVE_ID, USER_ID)
        .exchange()
        .expectStatus().isOk()
        .expectBody().isEmpty();

    Mockito.verify(onboardingOutcomeMediatorService, Mockito.only())
        .processOnboardingOutcome(INITIATIVE_ID, USER_ID);
  }

  @Test
  void shouldReturnServerErrorWhenServiceFails() {
    Mockito.when(onboardingOutcomeMediatorService.processOnboardingOutcome(INITIATIVE_ID, USER_ID))
        .thenReturn(Mono.error(new RuntimeException("boom")));

    webTestClient.post()
        .uri(ONBOARDING_OUTCOME_PATH, INITIATIVE_ID, USER_ID)
        .exchange()
        .expectStatus().is5xxServerError();

    Mockito.verify(onboardingOutcomeMediatorService, Mockito.only())
        .processOnboardingOutcome(INITIATIVE_ID, USER_ID);
  }
}
