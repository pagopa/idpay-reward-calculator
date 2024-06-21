package it.gov.pagopa.reward.dto.mapper.trx;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RewardCountersMapper {

    public RewardCounters apply(UserInitiativeCounters userInitiativeCounters, RewardTransactionDTO ruleEngineResult, InitiativeConfig initiativeConfig) {
        RewardCounters rewardCounters = new RewardCounters();
        rewardCounters.setVersion(userInitiativeCounters.getVersion());
        rewardCounters.setExhaustedBudget(userInitiativeCounters.isExhaustedBudget());
        rewardCounters.setTrxNumber(userInitiativeCounters.getTrxNumber());
        rewardCounters.setTotalRewardCents(userInitiativeCounters.getTotalRewardCents());
        rewardCounters.setInitiativeBudgetCents(initiativeConfig.getBeneficiaryBudgetCents());
        rewardCounters.setTotalAmountCents(userInitiativeCounters.getTotalAmountCents());

        rewardCounters.setDailyCounters(extractInvolved(userInitiativeCounters.getDailyCounters(), RewardConstants.dayDateFormatter.format(ruleEngineResult.getTrxChargeDate())));
        rewardCounters.setWeeklyCounters(extractInvolved(userInitiativeCounters.getWeeklyCounters(), RewardConstants.weekDateFormatter.format(ruleEngineResult.getTrxChargeDate())));
        rewardCounters.setMonthlyCounters(extractInvolved(userInitiativeCounters.getMonthlyCounters(), RewardConstants.monthDateFormatter.format(ruleEngineResult.getTrxChargeDate())));
        rewardCounters.setYearlyCounters(extractInvolved(userInitiativeCounters.getYearlyCounters(), RewardConstants.yearDateFormatter.format(ruleEngineResult.getTrxChargeDate())));

        return rewardCounters;
    }
    private Map<String, Counters> extractInvolved(Map<String, Counters> counters, String key) {
        Counters involvedCounter = counters.get(key);
        return involvedCounter !=null ? Map.of(key, involvedCounter) : null;
    }
}
