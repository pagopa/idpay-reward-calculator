package it.gov.pagopa.reward.service.lookup;

import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * This component take a EvaluationDto and save into DB
 * <ol>
 *     <li>retrieve if userId is present in DB</li>
 *     <li>evaluate message</li>
 *     <li>save into DB</li>
 * </ol>*/

public interface OnboardingOutcomeMediatorService {
    void execute(Flux<Message<String>> messageFlux);
}

