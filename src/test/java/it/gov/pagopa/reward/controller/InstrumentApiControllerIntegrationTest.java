package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import it.gov.pagopa.reward.connector.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.enums.HpanInitiativeStatus;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.test.fakers.HpanInitiativesFaker;
import org.apache.commons.lang3.function.FailableConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;

@TestPropertySource(
        properties = {
                "logging.level.it.gov.pagopa.reward=WARN",
                "logging.level.it.gov.pagopa.common.web.exception.ErrorManager=OFF",
                "logging.level.it.gov.pagopa.common.reactive.utils.PerformanceLogger=WARN"
        })
public class InstrumentApiControllerIntegrationTest extends BaseApiControllerIntegrationTest {
    @SpyBean
    private HpanInitiativesRepository hpanInitiativesRepositorySpy;

    private final List<FailableConsumer<Integer, Exception>> useCases = new ArrayList<>();

    @Test
    void test() {

        int N = Math.max(useCases.size(), 50);

        configureSpy();

        baseParallelismTest(N, useCases);
    }

    private void configureSpy() {
        Mockito.doThrow(new RuntimeException("DUMMY_EXCEPTION"))
                        .when(hpanInitiativesRepositorySpy)
                .setStatus(Mockito.argThat(arg -> arg.startsWith("USERDUMMY")), Mockito.argThat(arg -> arg.startsWith("INITIATIVEDUMMY")), Mockito.any());
    }

    //region useCases
    {
        //usecase 0: cancel and reactivate instruments
        useCases.add(i -> {
            HpanInitiatives hpanInitiatives = HpanInitiativesFaker.mockInstance(i);
            hpanInitiativesRepositorySpy.save(hpanInitiatives).block();
            OnboardedInitiative oiInitial = retrieveOnboardedInitiative(i, hpanInitiatives);

            String initiativeId = hpanInitiatives.getOnboardedInitiatives().get(0).getInitiativeId();

            extractResponse(cancelInstruments(webTestClient, hpanInitiatives.getUserId(), initiativeId), HttpStatus.NO_CONTENT, null);

            HpanInitiatives hpanInitiativesFirstCall = hpanInitiativesRepositorySpy.findById(hpanInitiatives.getHpan()).block();

            Assertions.assertNotNull(hpanInitiativesFirstCall);
            OnboardedInitiative oiFirstCancel = retrieveOnboardedInitiative(i, hpanInitiativesFirstCall);
            assertionsOnboardedInitiative(oiInitial, oiFirstCancel, HpanInitiativeStatus.INACTIVE);

            //resend the request
            extractResponse(cancelInstruments(webTestClient, hpanInitiatives.getUserId(), initiativeId), HttpStatus.NO_CONTENT, null);
            HpanInitiatives hpanInitiativesSecondCall = hpanInitiativesRepositorySpy.findById(hpanInitiatives.getHpan()).block();
            Assertions.assertEquals(hpanInitiativesFirstCall, hpanInitiativesSecondCall);

            //reactivate
            extractResponse(reactivateInstruments(webTestClient, hpanInitiatives.getUserId(), initiativeId), HttpStatus.NO_CONTENT, null);
            HpanInitiatives hpanInitiativesReactivate = hpanInitiativesRepositorySpy.findById(hpanInitiatives.getHpan()).block();
            Assertions.assertNotNull(hpanInitiativesReactivate);
            Assertions.assertNotEquals(hpanInitiativesFirstCall, hpanInitiativesReactivate);

            OnboardedInitiative oiReactivate = retrieveOnboardedInitiative(i, hpanInitiativesReactivate);
            assertionsOnboardedInitiative(oiFirstCancel, oiReactivate, HpanInitiativeStatus.ACTIVE);
        });

        //usecase 1: Generic error
        useCases.add(i -> {
            ErrorDTO errorDTOResult = extractResponse(cancelInstruments(webTestClient, "USERDUMMY_%d".formatted(i), "INITIATIVEDUMMY_%d".formatted(i)),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorDTO.class);

            ErrorDTO errorDTOExpected = new ErrorDTO("Error", "Something gone wrong");
            Assertions.assertEquals(errorDTOExpected, errorDTOResult);

            ErrorDTO errorDTORollbackResult = extractResponse(reactivateInstruments(webTestClient, "USERDUMMY_%d".formatted(i), "INITIATIVEDUMMY_%d".formatted(i)),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorDTO.class);

            Assertions.assertEquals(errorDTOExpected, errorDTORollbackResult);
        });
    }

    private void assertionsOnboardedInitiative(OnboardedInitiative oiBeforeCall, OnboardedInitiative oiAfterCall, HpanInitiativeStatus expectedStatus) {
        Assertions.assertEquals(oiBeforeCall.getInitiativeId(), oiAfterCall.getInitiativeId());
        Assertions.assertEquals(oiBeforeCall.getAcceptanceDate(), oiAfterCall.getAcceptanceDate());
        Assertions.assertNotEquals(oiBeforeCall.getStatus(), oiAfterCall.getStatus());
        Assertions.assertEquals(expectedStatus, oiAfterCall.getStatus());
        Assertions.assertEquals(oiBeforeCall.getLastEndInterval(), oiAfterCall.getLastEndInterval());
        Assertions.assertEquals(oiBeforeCall.getActiveTimeIntervals(), oiAfterCall.getActiveTimeIntervals());
    }

    private OnboardedInitiative retrieveOnboardedInitiative(Integer i, HpanInitiatives hpanInitiatives) {
        return hpanInitiatives.getOnboardedInitiatives()
                .stream()
                .filter(onboardedInitiative -> onboardedInitiative.getInitiativeId().equals("INITIATIVE_%d".formatted(i)))
                .findFirst().orElse(null);
    }

    //region API invokes
    public static WebTestClient.ResponseSpec cancelInstruments(WebTestClient webTestClient, String userId, String initiativeId){
        return webTestClient.delete()
                .uri(uriBuilder -> uriBuilder.path("/paymentinstrument/{userId}/{initiativeId}")
                        .build(userId, initiativeId))
                .exchange();
    }

    public static WebTestClient.ResponseSpec reactivateInstruments(WebTestClient webTestClient, String userId, String initiativeId){
        return webTestClient.put()
                .uri(uriBuilder -> uriBuilder.path("/paymentinstrument/{userId}/{initiativeId}/reactivate")
                        .build(userId, initiativeId))
                .exchange();
    }
    //endregion
}