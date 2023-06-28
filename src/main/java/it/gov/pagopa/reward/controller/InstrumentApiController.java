package it.gov.pagopa.reward.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

@RequestMapping("/paymentinstrument")
public interface InstrumentApiController {

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{userId}/{initiativeId}")
    Mono<Void> cancelInstruments(@PathVariable("userId") String userId,
                                 @PathVariable("initiativeId") String initiativeId);
}
