package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

import java.util.List;

public interface InitiativesEvaluatorFacadeService {
    /** It will evaluate initiative budget and rules, then it will update user budgets */
    Mono<RewardTransactionDTO> evaluateAndUpdateBudget(TransactionDTO trx, List<String> initiatives);

    /** It will return a preview of the reward evaluating initiative budget and rules, without budget updates */
    Pair<UserInitiativeCounters, RewardTransactionDTO> evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCounters userCounters);
}
