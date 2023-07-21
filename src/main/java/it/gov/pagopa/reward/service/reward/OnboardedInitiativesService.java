package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.OnboardingInfo;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * This component will retrieve initiatives to which the input hpan has been enabled
 * */
public interface OnboardedInitiativesService {
    Flux<InitiativeConfig> getInitiatives(TransactionDTO trx);
    Mono<Pair<InitiativeConfig, OnboardingInfo>> isOnboarded(String hpan, OffsetDateTime trxDate, String initiativeId);
}
