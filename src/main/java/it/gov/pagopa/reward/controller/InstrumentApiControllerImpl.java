package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.service.recess.InstrumentApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class InstrumentApiControllerImpl implements  InstrumentApiController{

    private final InstrumentApiService instrumentApiService;

    public InstrumentApiControllerImpl(InstrumentApiService instrumentApiService) {
        this.instrumentApiService = instrumentApiService;
    }
    @Override
    public Mono<Void> cancelInstruments(@RequestBody HpanInitiativeBulkDTO hpanInitiativeBulkDTO) {
        log.info("[SYNC_CANCEL_INSTRUMENTS] Requesting to cancel instruments {}", hpanInitiativeBulkDTO);

        return PerformanceLogger.logTimingFinally("SYNC_CANCEL_INSTRUMENTS",
                instrumentApiService.cancelInstruments(hpanInitiativeBulkDTO).then()
                        .onErrorResume(e -> e instanceof IllegalArgumentException ?
                                Mono.error(new ClientExceptionNoBody(HttpStatus.BAD_REQUEST, e.getMessage(), e))
                                : Mono.error(e)),
                        hpanInitiativeBulkDTO.toString());
    }
}
