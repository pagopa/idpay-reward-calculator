package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.common.web.exception.ClientExceptionNoBody;
import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import it.gov.pagopa.reward.service.recess.InstrumentApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class InstrumentApiControllerImpl implements  InstrumentApiController{

    private final InstrumentApiService instrumentApiService;

    public InstrumentApiControllerImpl(InstrumentApiService instrumentApiService) {
        this.instrumentApiService = instrumentApiService;
    }
    @Override
    public Mono<Void> cancelInstrument(@RequestBody HpanInitiativeBulkDTO hpanInitiativeBulkDTO) {
        log.info("[SYNC_CANCEL_INSTRUMENTS] Requesting to cancel instruments {}", hpanInitiativeBulkDTO);

        return PerformanceLogger.logTimingFinally("SYNC_CANCEL_INSTRUMENTS",
                instrumentApiService.cancelInstrument(hpanInitiativeBulkDTO).then()
                        .doOnError(e ->
                               Mono.error(new ClientExceptionNoBody(HttpStatus.INTERNAL_SERVER_ERROR, "Something gone wrong while cancelling instruments", e))),
                        hpanInitiativeBulkDTO.toString());
    }
}
