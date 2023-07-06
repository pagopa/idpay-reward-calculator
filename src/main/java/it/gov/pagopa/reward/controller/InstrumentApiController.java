package it.gov.pagopa.reward.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/paymentinstrument")
public interface InstrumentApiController {

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{userId}/{initiativeId}")
    Mono<Void> disableUserInitiativeInstruments(@PathVariable("userId") String userId,
                                                @PathVariable("initiativeId") String initiativeId);

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("{userId}/{initiativeId}/reactivate")
    Mono<Void> enableUserInitiativeInstruments(@PathVariable("userId") String userId,
                                               @PathVariable("initiativeId") String initiativeId);
}
