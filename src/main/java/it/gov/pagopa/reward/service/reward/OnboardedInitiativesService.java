package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.TransactionDTO;
import reactor.core.publisher.Flux;

/**
 * This component will retrieve initiatives to which the input hpan has been enabled
 * */
public interface OnboardedInitiativesService {
    Flux<String> getInitiatives(TransactionDTO trx);
}
