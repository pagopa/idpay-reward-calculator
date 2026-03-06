package it.gov.pagopa.reward.connector.rest.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class OnboardingWorkflowConnectorImplTest {

    private static final String USER_ID = "USER_1";
    private static final String INITIATIVE_ID = "INITIATIVE_1";
    private static final String BASE_URL = "http://test-onboarding-workflow";
    private static final String GET_STATUS_PATH = "/idpay/onboarding/{initiativeId}/{userId}/status";

    private ExchangeFunction exchangeFunction;
    private OnboardingWorkflowConnectorImpl connector;

    @BeforeEach
    void setUp() {
        // ExchangeFunction is replaced per test; initialised with a no-op here
        exchangeFunction = request -> Mono.empty();
        connector = buildConnector(request -> exchangeFunction.exchange(request));
    }

    private OnboardingWorkflowConnectorImpl buildConnector(ExchangeFunction ef) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(ef);
        return new OnboardingWorkflowConnectorImpl(builder, BASE_URL, GET_STATUS_PATH);
    }

    // ─── happy path ─────────────────────────────────────────────────────────────

    @Test
    void getOnboardingStatus_thenReturnOk_withoutFamilyId() {
        connector = buildConnector(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"status\":\"ONBOARDING_OK\"}")
                        .build()));

        StepVerifier.create(connector.getOnboardingStatus(USER_ID, INITIATIVE_ID))
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo("ONBOARDING_OK");
                    assertThat(response.familyId()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void getOnboardingStatus_thenReturnOk_withFamilyId() {
        connector = buildConnector(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"status\":\"ONBOARDING_OK\",\"familyId\":\"FAM_1\"}")
                        .build()));

        StepVerifier.create(connector.getOnboardingStatus(USER_ID, INITIATIVE_ID))
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo("ONBOARDING_OK");
                    assertThat(response.familyId()).isEqualTo("FAM_1");
                })
                .verifyComplete();
    }

    // ─── non-ONBOARDING_OK status ────────────────────────────────────────────────

    @Test
    void getOnboardingStatus_thenReturnKo_status() {
        connector = buildConnector(request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"status\":\"ONBOARDING_KO\"}")
                        .build()));

        // connector should still emit the raw DTO; filtering is done in the service
        StepVerifier.create(connector.getOnboardingStatus(USER_ID, INITIATIVE_ID))
                .assertNext(response -> assertThat(response.status()).isEqualTo("ONBOARDING_KO"))
                .verifyComplete();
    }

    // ─── error / 404 paths ───────────────────────────────────────────────────────

    @Test
    void getOnboardingStatus_thenReturnEmpty_onNotFound() {
        connector = buildConnector(request -> Mono.just(
                ClientResponse.create(HttpStatus.NOT_FOUND)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("")
                        .build()));

        StepVerifier.create(connector.getOnboardingStatus(USER_ID, INITIATIVE_ID))
                .verifyComplete(); // empty Mono expected
    }

    @Test
    void getOnboardingStatus_thenReturnEmpty_onServerError() {
        connector = buildConnector(request -> Mono.just(
                ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"error\":\"unexpected\"}")
                        .build()));

        StepVerifier.create(connector.getOnboardingStatus(USER_ID, INITIATIVE_ID))
                .verifyComplete(); // swallowed with log → empty Mono
    }

    @Test
    void getOnboardingStatus_thenReturnEmpty_onConnectionError() {
        connector = buildConnector(request -> Mono.error(new RuntimeException("Connection refused")));

        StepVerifier.create(connector.getOnboardingStatus(USER_ID, INITIATIVE_ID))
                .verifyComplete(); // swallowed with log → empty Mono
    }

    // ─── URL construction ────────────────────────────────────────────────────────

    @Test
    void getOnboardingStatus_usesCorrectUri() {
        connector = buildConnector(request -> {
            String path = request.url().getPath();
            assertThat(path)
                    .isEqualTo("/idpay/onboarding/" + INITIATIVE_ID + "/" + USER_ID + "/status");
            return Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body("{\"status\":\"ONBOARDING_OK\"}")
                            .build());
        });

        connector.getOnboardingStatus(USER_ID, INITIATIVE_ID).block();
    }
}
