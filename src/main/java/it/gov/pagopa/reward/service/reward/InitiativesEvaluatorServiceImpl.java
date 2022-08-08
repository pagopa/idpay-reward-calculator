package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InitiativesEvaluatorServiceImpl implements InitiativesEvaluatorService {

    private final RuleEngineService ruleEngineService;

    public InitiativesEvaluatorServiceImpl(RuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }

    @Override
    public RewardTransactionDTO evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCounters userCounters) {
        List<String> notExhaustedInitiatives = new ArrayList<>();
        Map<String, List<String>> rejectedInitiativesForBudget = new HashMap<>();
        initiatives.forEach(initiativeId -> {
            InitiativeCounters initiativeCounters = userCounters.getInitiatives().get(initiativeId);
            if(initiativeCounters != null && initiativeCounters.isExhaustedBudget()) {
                rejectedInitiativesForBudget.put(initiativeId, List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));
            } else {
                notExhaustedInitiatives.add(initiativeId);
            }
        });
        RewardTransactionDTO trxRewarded = ruleEngineService.applyRules(trx, notExhaustedInitiatives, userCounters);
        if(trxRewarded!=null){
            trxRewarded.getInitiativeRejectionReasons().putAll(rejectedInitiativesForBudget);
            return trxRewarded;
        } else {
            return null;
        }
    }
}
