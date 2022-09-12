package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
            // exhausted initiative to be considered in case of REVERSAL in order to
            if(initiativeCounters != null && initiativeCounters.isExhaustedBudget() && !isExhausted2Reverse(trx, initiativeId)) {
                rejectedInitiativesForBudget.put(initiativeId, List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED));
            } else {
                notExhaustedInitiatives.add(initiativeId);
            }
        });
        RewardTransactionDTO trxRewarded = ruleEngineService.applyRules(trx, notExhaustedInitiatives, userCounters);
        if(trxRewarded!=null){
            trxRewarded.getInitiativeRejectionReasons().putAll(rejectedInitiativesForBudget);
        }
        return trxRewarded;
    }

    /** if REVERSAL, exhausted initiative considered only if they have already rewarded */
    private boolean isExhausted2Reverse(TransactionDTO trx, String initiativeId) {
        return OperationType.REVERSAL.equals(trx.getOperationTypeTranscoded()) && trx.getReversalInfo()!=null && BigDecimal.ZERO.compareTo(trx.getReversalInfo().getPreviousRewards().get(initiativeId).getAccruedReward())<0;
    }
}
