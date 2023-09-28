package it.gov.pagopa.reward.service.commands.ops;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface DeleteInitiativeService {
    Mono<String> execute(String initiativeId);
    Mono<Void> removedAfterInitiativeDeletion(Duration delayDuration);
}
