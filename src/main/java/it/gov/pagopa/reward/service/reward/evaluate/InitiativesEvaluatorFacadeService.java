package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import reactor.core.publisher.Mono;

import java.util.List;

public interface InitiativesEvaluatorFacadeService {
    Mono<RewardTransactionDTO> evaluate(TransactionDTO trx, List<String> initiatives);
}
