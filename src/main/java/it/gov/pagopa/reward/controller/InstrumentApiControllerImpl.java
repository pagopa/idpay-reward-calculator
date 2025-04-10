package it.gov.pagopa.reward.controller;

import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.reward.service.recess.InstrumentApiService;
import lombok.extern.slf4j.Slf4j;
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
    public Mono<Void> disableUserInitiativeInstruments(String userId, String initiativeId) {
        log.info("[SYNC_DISABLE_INSTRUMENTS] Requesting to disable instruments for the user {} to the initiative {}", userId, initiativeId);

        return PerformanceLogger.logTimingFinally("SYNC_DISABLE_INSTRUMENTS",
                instrumentApiService.disableUserInitiativeInstruments(userId, initiativeId),
                        userId+"_"+initiativeId);
    }

    @Override
    public Mono<Void> enableUserInitiativeInstruments(String userId, String initiativeId) {
        log.info("[SYNC_ENABLE_INSTRUMENTS] Requesting to enable instruments for the user {} to the initiative {}", userId, initiativeId);

        return PerformanceLogger.logTimingFinally("SYNC_ENABLE_INSTRUMENTS",
                instrumentApiService.enableUserInitiativeInstruments(userId, initiativeId),
                userId+"_"+initiativeId);
    }
}
