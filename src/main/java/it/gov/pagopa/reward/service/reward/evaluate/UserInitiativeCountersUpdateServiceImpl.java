package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class UserInitiativeCountersUpdateServiceImpl implements UserInitiativeCountersUpdateService {

    public static final DateTimeFormatter dayDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter weekDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-W", Locale.ITALY);
    public static final DateTimeFormatter monthDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    public static final DateTimeFormatter yearDateFormatter = DateTimeFormatter.ofPattern("yyyy");

    //region getter's for constants due to allow Drools recognize and obtain them
    @SuppressWarnings("unused")
    public static DateTimeFormatter getDayDateFormatter() {
        return dayDateFormatter;
    }

    @SuppressWarnings("unused")
    public static DateTimeFormatter getWeekDateFormatter() {
        return weekDateFormatter;
    }

    @SuppressWarnings("unused")
    public static DateTimeFormatter getMonthDateFormatter() {
        return monthDateFormatter;
    }

    @SuppressWarnings("unused")
    public static DateTimeFormatter getYearDateFormatter() {
        return yearDateFormatter;
    }
    //endregion

    private final RewardContextHolderService rewardContextHolderService;

    private final List<String> justTrxCountRejectionReason = List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason());

    public UserInitiativeCountersUpdateServiceImpl(RewardContextHolderService rewardContextHolderService) {
        this.rewardContextHolderService = rewardContextHolderService;
    }

    @Override
    public void update(UserInitiativeCounters userInitiativeCounters, RewardTransactionDTO ruleEngineResult) {
        Stream.concat(
                ruleEngineResult.getRewards().entrySet().stream(),
                // when the TrxCount is the only rejection reason, we have to update the counters
                ruleEngineResult.getInitiativeRejectionReasons().entrySet().stream()
                        .filter(e -> isJustTrxCountRejection(e.getValue()))
                        .map(e-> Pair.of(e.getKey(), new Reward(e.getKey(), null, BigDecimal.ZERO)))
        ).forEach(e -> {
            String initiativeId = e.getKey();
            Reward reward = e.getValue();

            boolean justTrxCountRejection = isJustTrxCountRejection(ruleEngineResult, initiativeId);

            InitiativeConfig initiativeConfig = rewardContextHolderService.getInitiativeConfig(initiativeId);
            InitiativeCounters initiativeCounter = userInitiativeCounters.getInitiatives()
                    .computeIfAbsent(initiativeId, k -> InitiativeCounters.builder().initiativeId(k).build());

            if (isRefundedReward(initiativeId, ruleEngineResult) || isRewardedInitiative(reward) || justTrxCountRejection) {
                evaluateInitiativeBudget(reward, initiativeConfig, initiativeCounter);
                final BigDecimal previousRewards = ruleEngineResult.getRefundInfo() != null ? Optional.ofNullable(ruleEngineResult.getRefundInfo().getPreviousRewards().get(initiativeId)).map(RefundInfo.PreviousReward::getAccruedReward).orElse(null) : null;
                updateCounters(initiativeCounter, ruleEngineResult.getOperationTypeTranscoded(), reward, previousRewards, ruleEngineResult.getAmount(), ruleEngineResult.getEffectiveAmount(), justTrxCountRejection);
                updateTemporalCounters(initiativeCounter, ruleEngineResult.getOperationTypeTranscoded(), reward, ruleEngineResult, previousRewards, initiativeConfig, justTrxCountRejection);
            }

            /* set RewardCounters in RewardTransactionDTO object */
            reward.setCounters(mapRewardCounters(initiativeCounter, initiativeConfig));
        });
    }

    private void evaluateInitiativeBudget(Reward reward, InitiativeConfig initiativeConfig, InitiativeCounters initiativeCounter) {
        initiativeCounter.setExhaustedBudget(initiativeConfig.getBeneficiaryBudget() != null && initiativeCounter.getTotalReward().add(reward.getAccruedReward()).compareTo(initiativeConfig.getBeneficiaryBudget()) > -1);
        if (initiativeCounter.isExhaustedBudget()) {
            BigDecimal newAccruedReward = initiativeConfig.getBeneficiaryBudget().subtract(initiativeCounter.getTotalReward()).setScale(2, RoundingMode.HALF_DOWN);
            reward.setCapped(newAccruedReward.compareTo(reward.getAccruedReward()) != 0);
            reward.setAccruedReward(newAccruedReward);
        }
    }

    // capped rewards due to reward limit could bright to refund having reward 0, but the total amount would change
    private boolean isRefundedReward(String initiativeId, RewardTransactionDTO ruleEngineResult) {
        return OperationType.REFUND.equals(ruleEngineResult.getOperationTypeTranscoded()) &&
                ruleEngineResult.getRefundInfo() != null &&
                ruleEngineResult.getRefundInfo().getPreviousRewards() != null &&
                ruleEngineResult.getRefundInfo().getPreviousRewards().get(initiativeId) != null;
    }

    private boolean isRewardedInitiative(Reward initiativeReward) {
        return initiativeReward != null && initiativeReward.getAccruedReward().compareTo(BigDecimal.ZERO) != 0;
    }

    private boolean isJustTrxCountRejection(RewardTransactionDTO ruleEngineResult, String initiativeId) {
        return isJustTrxCountRejection(ruleEngineResult.getInitiativeRejectionReasons().get(initiativeId));
    }

    private boolean isJustTrxCountRejection(List<String> ruleEngineResult) {
        return justTrxCountRejectionReason.equals(ruleEngineResult);
    }

    private void updateCounters(Counters counters, OperationType operationType, Reward reward, BigDecimal previousRewards, BigDecimal amount, BigDecimal effectiveAmount, boolean justTrxCountRejection) {
        if(justTrxCountRejection){
            handleTrxCountRejection(counters, operationType, effectiveAmount);
        } else {
            if (previousRewards == null) {
                counters.setTrxNumber(counters.getTrxNumber() + 1);
                counters.setTotalAmount(counters.getTotalAmount().add(effectiveAmount));
            } else {
                if (previousRewards.compareTo(BigDecimal.ZERO)>0) {
                    counters.setTotalAmount(counters.getTotalAmount().subtract(amount));
                    // if previous reward was 0, due to CAP, when reverting it, it should not decrease the counters
                    if (BigDecimal.ZERO.compareTo(previousRewards.add(reward.getAccruedReward())) == 0) {
                        counters.setTrxNumber(counters.getTrxNumber() - 1);
                        counters.setTotalAmount(counters.getTotalAmount().subtract(effectiveAmount));
                        reward.setCompleteRefund(true);
                    }
                }
            }
            counters.setTotalReward(counters.getTotalReward().add(reward.getAccruedReward()));
        }
    }

    private static void handleTrxCountRejection(Counters counters, OperationType operationType, BigDecimal effectiveAmount) {
        // charge op
        if (OperationType.CHARGE.equals(operationType)) {
            counters.setTrxNumber(counters.getTrxNumber() + 1);
        } else {
            // complete refund
            if (BigDecimal.ZERO.compareTo(effectiveAmount)==0) {
                counters.setTrxNumber(counters.getTrxNumber() - 1);
            }
        }
    }

    private void updateTemporalCounters(InitiativeCounters initiativeCounters, OperationType operationType, Reward initiativeReward, RewardTransactionDTO ruleEngineResult, BigDecimal previousRewards, InitiativeConfig initiativeConfig, boolean justTrxCountRejection) {
        if (initiativeConfig.isDailyThreshold()) {
            updateTemporalCounter(initiativeCounters.getDailyCounters(), dayDateFormatter, ruleEngineResult, operationType, previousRewards, initiativeReward, justTrxCountRejection);
        }
        if (initiativeConfig.isWeeklyThreshold()) {
            updateTemporalCounter(initiativeCounters.getWeeklyCounters(), weekDateFormatter, ruleEngineResult, operationType, previousRewards, initiativeReward, justTrxCountRejection);
        }
        if (initiativeConfig.isMonthlyThreshold()) {
            updateTemporalCounter(initiativeCounters.getMonthlyCounters(), monthDateFormatter, ruleEngineResult, operationType, previousRewards, initiativeReward, justTrxCountRejection);
        }
        if (initiativeConfig.isYearlyThreshold()) {
            updateTemporalCounter(initiativeCounters.getYearlyCounters(), yearDateFormatter, ruleEngineResult, operationType, previousRewards, initiativeReward, justTrxCountRejection);
        }
    }

    private void updateTemporalCounter(Map<String, Counters> periodicalMap, DateTimeFormatter periodicalKeyFormatter, RewardTransactionDTO ruleEngineResult, OperationType operationType, BigDecimal previousRewards, Reward initiativeReward, boolean justTrxCountRejection) {
        updateCounters(periodicalMap.computeIfAbsent(periodicalKeyFormatter.format(ruleEngineResult.getTrxChargeDate()), k -> new Counters()), operationType, initiativeReward, previousRewards, ruleEngineResult.getAmount(), ruleEngineResult.getEffectiveAmount(), justTrxCountRejection);
    }

    private RewardCounters mapRewardCounters(InitiativeCounters initiativeCounters, InitiativeConfig initiativeConfig) {
        RewardCounters rewardCounters = new RewardCounters();
        rewardCounters.setExhaustedBudget(initiativeCounters.isExhaustedBudget());
        rewardCounters.setTrxNumber(initiativeCounters.getTrxNumber());
        rewardCounters.setTotalReward(initiativeCounters.getTotalReward());
        rewardCounters.setInitiativeBudget(initiativeConfig.getBeneficiaryBudget());
        rewardCounters.setTotalAmount(initiativeCounters.getTotalAmount());

        return rewardCounters;
    }
}
