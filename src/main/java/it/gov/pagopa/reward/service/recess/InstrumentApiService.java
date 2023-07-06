package it.gov.pagopa.reward.service.recess;

import reactor.core.publisher.Mono;

public interface InstrumentApiService {
    Mono<Void> disableUserInitiativeInstruments(String userId, String initiativeId);

    Mono<Void> enableUserInitiativeInstruments(String userId, String initiativeId);
}
