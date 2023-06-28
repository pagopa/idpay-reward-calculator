package it.gov.pagopa.reward.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/paymentinstrument")
public interface InstrumentApiController {

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{userId}/{initiativeId}")
    Mono<Void> cancelInstruments(@PathVariable("userId") String userId,
                                 @PathVariable("initiativeId") String initiativeId);

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{userId}/{initiativeId}")
    Mono<Void> rollbackInstruments(@PathVariable("userId") String userId,
                                 @PathVariable("initiativeId") String initiativeId);
}
