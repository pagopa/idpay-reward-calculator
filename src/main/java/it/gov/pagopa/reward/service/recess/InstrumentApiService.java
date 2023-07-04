package it.gov.pagopa.reward.service.recess;

import reactor.core.publisher.Mono;

public interface InstrumentApiService {
    Mono<Void> cancelInstruments(String userId, String initiativeId);

    Mono<Void> reactivateInstruments(String userId, String initiativeId);
}
