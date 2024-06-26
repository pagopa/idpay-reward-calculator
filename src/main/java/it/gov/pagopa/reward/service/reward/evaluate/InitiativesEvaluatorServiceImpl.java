package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
    public RewardTransactionDTO evaluateInitiativesBudgetAndRules(TransactionDTO trx, List<String> initiatives, UserInitiativeCountersWrapper userCounters) {
        List<String> notExhaustedInitiatives = new ArrayList<>();
        Map<String, List<String>> rejectedInitiativesForBudget = new HashMap<>();
        initiatives.forEach(initiativeId -> {
            UserInitiativeCounters userInitiativeCounters = userCounters.getInitiatives().get(initiativeId);
            // exhausted initiative to be considered in case of REFUND in order to
            if(userInitiativeCounters != null && userInitiativeCounters.isExhaustedBudget() && !isExhausted2Reverse(trx, initiativeId)) {
                rejectedInitiativesForBudget.put(initiativeId, List.of(RewardConstants.TRX_REJECTION_REASON_BUDGET_EXHAUSTED));
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

    /** if REFUND, exhausted initiative considered only if they have already rewarded */
    private boolean isExhausted2Reverse(TransactionDTO trx, String initiativeId) {
        return OperationType.REFUND.equals(trx.getOperationTypeTranscoded()) &&
                trx.getRefundInfo() != null &&
                trx.getRefundInfo().getPreviousRewards().get(initiativeId) != null &&
                (
                        0L < trx.getRefundInfo().getPreviousRewards().get(initiativeId).getAccruedRewardCents()
                                || (
                                !CollectionUtils.isEmpty(trx.getRefundInfo().getPreviousTrxs()) &&
                                        List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason()).equals(trx.getRefundInfo().getPreviousTrxs().get(0).getInitiativeRejectionReasons().get(initiativeId))
                        )
                );
    }
}
