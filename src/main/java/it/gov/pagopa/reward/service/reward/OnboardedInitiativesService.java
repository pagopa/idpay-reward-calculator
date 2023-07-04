package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * This component will retrieve initiatives to which the input hpan has been enabled
 * */
public interface OnboardedInitiativesService {
    Flux<InitiativeConfig> getInitiatives(TransactionDTO trx);
    Mono<Boolean> isOnboarded(String hpan, OffsetDateTime trxDate, String initiativeId);
}
