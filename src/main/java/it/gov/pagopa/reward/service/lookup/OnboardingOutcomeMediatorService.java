package it.gov.pagopa.reward.service.lookup;

import it.gov.pagopa.reward.dto.EvaluationDTO;
import reactor.core.publisher.Mono;

/**
 * This component take a EvaluationDto and save into DB
 * <ol>
 *     <li>retrieve if userId is present in DB</li>
 *     <li>evaluate message</li>
 *     <li>save into DB</li>
 * </ol>*/

public interface OnboardingOutcomeMediatorService {
    Mono<EvaluationDTO> processOnboardingOutcome(EvaluationDTO payload);
}
