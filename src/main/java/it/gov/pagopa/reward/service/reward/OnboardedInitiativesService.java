package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.HpanInitiatives;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * This component will retrieve initiatives to which the input hpan has been enabled
 * */
public interface OnboardedInitiativesService {
    Flux<String> getInitiatives(TransactionDTO trx);
    Mono<HpanInitiatives> isOnboarded(String hpan, String initiativeId);
}
