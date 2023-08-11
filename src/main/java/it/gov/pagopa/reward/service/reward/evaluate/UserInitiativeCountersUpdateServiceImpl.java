package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.RewardCountersMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserInitiativeCountersUpdateServiceImpl implements UserInitiativeCountersUpdateService {

    //region getter's for constants due to allow Drools recognize and obtain them
    @SuppressWarnings("unused")
    public static DateTimeFormatter getDayDateFormatter() {
        return RewardConstants.dayDateFormatter;
    }

    @SuppressWarnings("unused")
    public static DateTimeFormatter getWeekDateFormatter() {
        return RewardConstants.weekDateFormatter;
    }

    @SuppressWarnings("unused")
    public static DateTimeFormatter getMonthDateFormatter() {
        return RewardConstants.monthDateFormatter;
    }

    @SuppressWarnings("unused")
    public static DateTimeFormatter getYearDateFormatter() {
        return RewardConstants.yearDateFormatter;
    }
    //endregion

    private final RewardContextHolderService rewardContextHolderService;
    private final RewardCountersMapper rewardCountersMapper;

    private final List<String> justTrxCountRejectionReason = List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason());

    public UserInitiativeCountersUpdateServiceImpl(RewardContextHolderService rewardContextHolderService, RewardCountersMapper rewardCountersMapper) {
        this.rewardContextHolderService = rewardContextHolderService;
        this.rewardCountersMapper = rewardCountersMapper;
    }

    @Override
    public Mono<RewardTransactionDTO> update(UserInitiativeCountersWrapper userInitiativeCountersWrapper, RewardTransactionDTO ruleEngineResult) {
        return Flux.fromIterable(ruleEngineResult.getRewards().values())
                        .flatMap(reward -> {
                            String initiativeId = reward.getInitiativeId();
                            capRewardToEffectiveAmount(ruleEngineResult, reward, initiativeId);

                            return rewardContextHolderService.getInitiativeConfig(initiativeId)
                                    .doOnNext(initiativeConfig -> {
                                        boolean justTrxCountRejection = isJustTrxCountRejection(ruleEngineResult, initiativeId);

                                        UserInitiativeCounters initiativeCounter = userInitiativeCountersWrapper.getInitiatives()
                                                .computeIfAbsent(initiativeId, k -> UserInitiativeCounters.builder(ruleEngineResult.getUserId(),initiativeConfig.getBeneficiaryType(), k).build());

                                        if (isRefundedReward(initiativeId, ruleEngineResult) || isRewardedInitiative(reward) || justTrxCountRejection) {
                                            evaluateInitiativeBudget(reward, initiativeConfig, initiativeCounter);
                                            final BigDecimal previousRewards = ruleEngineResult.getRefundInfo() != null ? Optional.ofNullable(ruleEngineResult.getRefundInfo().getPreviousRewards().get(initiativeId)).map(RefundInfo.PreviousReward::getAccruedReward).orElse(null) : null;
                                            initiativeCounter.setVersion(initiativeCounter.getVersion()+1L);
                                            updateCounters(initiativeCounter, ruleEngineResult.getOperationTypeTranscoded(), reward, previousRewards, ruleEngineResult.getAmount(), ruleEngineResult.getEffectiveAmount(), justTrxCountRejection);
                                            updateTemporalCounters(initiativeCounter, ruleEngineResult.getOperationTypeTranscoded(), reward, ruleEngineResult, previousRewards, initiativeConfig, justTrxCountRejection);
                                        }

                                        /* set RewardCounters in RewardTransactionDTO object */
                                        reward.setCounters(rewardCountersMapper.apply(initiativeCounter, ruleEngineResult, initiativeConfig));

                                        if (InitiativeGeneralDTO.BeneficiaryTypeEnum.NF.equals(initiativeConfig.getBeneficiaryType())) {
                                            reward.setFamilyId(initiativeCounter.getEntityId());
                                        }
                                    });
                        })
                .then(Mono.just(ruleEngineResult));
    }

    private void capRewardToEffectiveAmount(RewardTransactionDTO ruleEngineResult, Reward reward, String initiativeId) {
        if(reward.getAccruedReward().compareTo(ruleEngineResult.getEffectiveAmount() )> 0){
            log.warn("[REWARD] The following initiative calculated a reward greater than the amount: initiativeId={}, amount={}, reward={}, trxId={}",
                    initiativeId, ruleEngineResult.getEffectiveAmount(), reward.getAccruedReward(), ruleEngineResult.getId());

            reward.setAccruedReward(ruleEngineResult.getEffectiveAmount());
        }
    }

    private void evaluateInitiativeBudget(Reward reward, InitiativeConfig initiativeConfig, UserInitiativeCounters initiativeCounter) {
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
            handleTrxCountRejection(counters, operationType, effectiveAmount, reward);
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

    private static void handleTrxCountRejection(Counters counters, OperationType operationType, BigDecimal effectiveAmount, Reward reward) {
        // charge op
        if (OperationType.CHARGE.equals(operationType)) {
            counters.setTrxNumber(counters.getTrxNumber() + 1);
        } else {
            // complete refund
            if (BigDecimal.ZERO.compareTo(effectiveAmount)==0) {
                counters.setTrxNumber(counters.getTrxNumber() - 1);
                reward.setCompleteRefund(true);
            }
        }
    }

    private void updateTemporalCounters(UserInitiativeCounters userInitiativeCounters, OperationType operationType, Reward initiativeReward, RewardTransactionDTO ruleEngineResult, BigDecimal previousRewards, InitiativeConfig initiativeConfig, boolean justTrxCountRejection) {
        if (initiativeConfig.isDailyThreshold()) {
            updateTemporalCounter(userInitiativeCounters.getDailyCounters(), RewardConstants.dayDateFormatter, ruleEngineResult, operationType, previousRewards, initiativeReward, justTrxCountRejection);
        }
        if (initiativeConfig.isWeeklyThreshold()) {
            updateTemporalCounter(userInitiativeCounters.getWeeklyCounters(), RewardConstants.weekDateFormatter, ruleEngineResult, operationType, previousRewards, initiativeReward, justTrxCountRejection);
        }
        if (initiativeConfig.isMonthlyThreshold()) {
            updateTemporalCounter(userInitiativeCounters.getMonthlyCounters(), RewardConstants.monthDateFormatter, ruleEngineResult, operationType, previousRewards, initiativeReward, justTrxCountRejection);
        }
        if (initiativeConfig.isYearlyThreshold()) {
            updateTemporalCounter(userInitiativeCounters.getYearlyCounters(), RewardConstants.yearDateFormatter, ruleEngineResult, operationType, previousRewards, initiativeReward, justTrxCountRejection);
        }
    }

    private void updateTemporalCounter(Map<String, Counters> periodicalMap, DateTimeFormatter periodicalKeyFormatter, RewardTransactionDTO ruleEngineResult, OperationType operationType, BigDecimal previousRewards, Reward initiativeReward, boolean justTrxCountRejection) {
        updateCounters(periodicalMap.computeIfAbsent(periodicalKeyFormatter.format(ruleEngineResult.getTrxChargeDate()), k -> new Counters()), operationType, initiativeReward, previousRewards, ruleEngineResult.getAmount(), ruleEngineResult.getEffectiveAmount(), justTrxCountRejection);
    }

}
