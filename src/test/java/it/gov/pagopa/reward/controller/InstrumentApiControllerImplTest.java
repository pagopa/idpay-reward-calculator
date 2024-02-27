package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.service.recess.InstrumentApiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {InstrumentApiController.class}) //TODO MOCKITO MONO VOID
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
        Void voidMock = Mockito.mock(Void.class);
//        Mockito.doNothing().when(instrumentApiServiceMock)
//                        .disableUserInitiativeInstruments(USERID, INITIATIVEID);


        Mockito.when(instrumentApiServiceMock.disableUserInitiativeInstruments(USERID, INITIATIVEID))
                .thenReturn(Mono.just(voidMock));

        webClient.delete()
                .uri(uriBuilder -> uriBuilder.path(disableInstrumentPath)
                        .build(USERID, INITIATIVEID))
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(instrumentApiServiceMock, Mockito.only()).disableUserInitiativeInstruments(Mockito.any(), Mockito.any());
    }

}