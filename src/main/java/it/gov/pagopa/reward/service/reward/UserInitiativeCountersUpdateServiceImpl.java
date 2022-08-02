package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserInitiativeCountersUpdateServiceImpl implements UserInitiativeCountersUpdateService {

    private static final DateTimeFormatter dayDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter weeklyDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-W", Locale.ITALY);
    private static final DateTimeFormatter monthDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter yearDateFormatter = DateTimeFormatter.ofPattern("yyyy");

    private final RewardContextHolderService rewardContextHolderService;

    private final List<String> justTrxCountRejectionReason = List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason());

    public UserInitiativeCountersUpdateServiceImpl(RewardContextHolderService rewardContextHolderService) {
        this.rewardContextHolderService = rewardContextHolderService;
    }


    @Override
    public void update(UserInitiativeCounters userInitiativeCounters, RewardTransactionDTO ruleEngineResult) { // TODO operation of storno
        ruleEngineResult.getRewards().forEach((initiativeId, reward) -> {
            if (isRewardedInitiative(reward) || isJustTrxCountRejection(ruleEngineResult, initiativeId)) {
                InitiativeConfig initiativeConfig = rewardContextHolderService.getInitiativeConfig(initiativeId);
                InitiativeCounters initiativeCounter = userInitiativeCounters.getInitiatives()
                        .computeIfAbsent(initiativeId, k -> InitiativeCounters.builder().initiativeId(k).build());
                if (initiativeCounter.getTotalReward().add(reward.getAccruedReward()).compareTo(initiativeConfig.getBudget()) > -1) {
                    initiativeCounter.setExhaustedBudget(true);
                    reward.setAccruedReward(initiativeConfig.getBudget().subtract(initiativeCounter.getTotalReward()));
                } else {
                    initiativeCounter.setExhaustedBudget(false);
                }
                updateCounters(initiativeCounter, reward, ruleEngineResult.getAmount());
                updateTemporalCounters(initiativeCounter, reward, ruleEngineResult, initiativeConfig);
            }
        });
    }

    private boolean isRewardedInitiative(Reward initiativeReward) {
        return initiativeReward != null && initiativeReward.getAccruedReward().compareTo(BigDecimal.ZERO) != 0;
    }

    private boolean isJustTrxCountRejection(RewardTransactionDTO ruleEngineResult, String initiativeId) {
        return justTrxCountRejectionReason.equals(ruleEngineResult.getInitiativeRejectionReasons().get(initiativeId));
    }

    private void updateCounters(Counters counters, Reward reward, BigDecimal amount) {
        counters.setTrxNumber(counters.getTrxNumber() + 1);
        counters.setTotalReward(counters.getTotalReward().add(reward.getAccruedReward()));
        counters.setTotalAmount(counters.getTotalAmount().add(amount));
    }

    private void updateTemporalCounters(InitiativeCounters initiativeCounters, Reward initiativeReward, RewardTransactionDTO ruleEngineResult, InitiativeConfig initiativeConfig) {
        if (initiativeConfig.isDailyThreshold()) {
            if(initiativeCounters.getDailyCounters()==null){
                initiativeCounters.setDailyCounters(new HashMap<>());
            }
            updateTemporalCounter(initiativeCounters.getDailyCounters(), dayDateFormatter, ruleEngineResult, initiativeReward);
        }
        if (initiativeConfig.isWeeklyThreshold()) {
            if(initiativeCounters.getWeeklyCounters()==null){
                initiativeCounters.setWeeklyCounters(new HashMap<>());
            }
            updateTemporalCounter(initiativeCounters.getWeeklyCounters(), weeklyDateFormatter, ruleEngineResult, initiativeReward);
        }
        if (initiativeConfig.isMonthlyThreshold()) {
            if(initiativeCounters.getMonthlyCounters()==null){
                initiativeCounters.setMonthlyCounters(new HashMap<>());
            }
            updateTemporalCounter(initiativeCounters.getMonthlyCounters(), monthDateFormatter, ruleEngineResult, initiativeReward);
        }
        if (initiativeConfig.isYearlyThreshold()) {
            if(initiativeCounters.getYearlyCounters()==null){
                initiativeCounters.setYearlyCounters(new HashMap<>());
            }
            updateTemporalCounter(initiativeCounters.getYearlyCounters(), yearDateFormatter, ruleEngineResult, initiativeReward);
        }
    }

    private void updateTemporalCounter(Map<String, Counters> periodicalMap, DateTimeFormatter periodicalKeyFormatter, RewardTransactionDTO ruleEngineResult, Reward initiativeReward) {
        updateCounters(periodicalMap.computeIfAbsent(periodicalKeyFormatter.format(ruleEngineResult.getTrxDate()), k->new Counters()), initiativeReward, ruleEngineResult.getAmount());
    }
}
