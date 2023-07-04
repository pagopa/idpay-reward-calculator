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
    public Mono<Void> cancelInstruments(String userId, String initiativeId) {
        log.info("[SYNC_CANCEL_INSTRUMENTS] Requesting to cancel instruments for user {} to the initiative {}", userId, initiativeId);

        return PerformanceLogger.logTimingFinally("SYNC_CANCEL_INSTRUMENTS",
                instrumentApiService.cancelInstruments(userId, initiativeId),
                        userId+"_"+initiativeId);
    }

    @Override
    public Mono<Void> reactivateInstruments(String userId, String initiativeId) {
        log.info("[SYNC_ROLLBACK_INSTRUMENTS] Requesting to reactivate instruments for user {} to the initiative {}", userId, initiativeId);

        return PerformanceLogger.logTimingFinally("SYNC_ROLLBACK_INSTRUMENTS",
                instrumentApiService.reactivateInstruments(userId, initiativeId),
                userId+"_"+initiativeId);
    }
}
