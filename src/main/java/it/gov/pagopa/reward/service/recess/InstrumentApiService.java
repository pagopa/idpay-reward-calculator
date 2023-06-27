package it.gov.pagopa.reward.service.recess;

import it.gov.pagopa.reward.dto.HpanInitiativeBulkDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface InstrumentApiService {
    Mono<List<String>> cancelInstruments(HpanInitiativeBulkDTO hpanInitiativeBulkDTO);
}
