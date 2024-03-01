package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.service.recess.InstrumentApiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {InstrumentApiController.class})
class InstrumentApiControllerImplTest {
    private static final String USERID = "USERID";
    private static final String INITIATIVEID = "INITIATIVEID";
    private final String disableInstrumentPath = "/paymentinstrument/{userId}/{initiativeId}";
    private final String enableInstrumentPath = "/paymentinstrument/{userId}/{initiativeId}/reactivate";


    @MockBean
    private InstrumentApiService instrumentApiServiceMock;
    @Autowired
    protected WebTestClient webClient;

    @Test
    void disableUserInitiativeInstruments_success() {

        Mockito.when(instrumentApiServiceMock.disableUserInitiativeInstruments(USERID, INITIATIVEID))
                .thenReturn(Mono.empty());

        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path(disableInstrumentPath)
                        .build(USERID, INITIATIVEID))
                .exchange()
                .expectStatus().isNoContent();

        Mockito.verify(instrumentApiServiceMock, Mockito.only()).disableUserInitiativeInstruments(Mockito.any(), Mockito.any());
    }

    @Test
    void enableUserInitiativeInstruments_success(){
        Mockito.when(instrumentApiServiceMock.enableUserInitiativeInstruments(USERID, INITIATIVEID))
                .thenReturn(Mono.empty());

        webClient.put()
                .uri(uriBuilder -> uriBuilder.path(enableInstrumentPath)
                        .build(USERID, INITIATIVEID))
                .exchange()
                .expectStatus().isNoContent();

        Mockito.verify(instrumentApiServiceMock, Mockito.only()).enableUserInitiativeInstruments(Mockito.any(), Mockito.any());

    }

}