package it.gov.pagopa.reward.controller;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

@RequestMapping("/reward")
public interface InstrumentApiController {

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/instrument")
    Mono<Void> cancelInstrument(@RequestBody HpanInitiativeBulkDTO hpanInitiativeBulkDTO);
}
