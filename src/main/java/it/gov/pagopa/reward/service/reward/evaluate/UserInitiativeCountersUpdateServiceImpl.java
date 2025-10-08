package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.BaseTransactionProcessed2LastTrxInfoDTOMapper;
import it.gov.pagopa.reward.dto.mapper.trx.RewardCountersMapper;
import it.gov.pagopa.reward.dto.trx.LastTrxInfoDTO;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final BaseTransactionProcessed2LastTrxInfoDTOMapper baseTransactionProcessed2LastTrxInfoDTOMapper;
    private final Duration lastTrxExpired;

    private final List<String> justTrxCountRejectionReason = List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason());

    public UserInitiativeCountersUpdateServiceImpl(RewardContextHolderService rewardContextHolderService,
                                                   RewardCountersMapper rewardCountersMapper,
                                                   BaseTransactionProcessed2LastTrxInfoDTOMapper baseTransactionProcessed2LastTrxInfoDTOMapper, @Value("${app.trx-counters.lastTrxExpired}") String lastTrxExpired) {
        this.rewardContextHolderService = rewardContextHolderService;
        this.rewardCountersMapper = rewardCountersMapper;
        this.baseTransactionProcessed2LastTrxInfoDTOMapper = baseTransactionProcessed2LastTrxInfoDTOMapper;
        this.lastTrxExpired = Duration.parse(lastTrxExpired);
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
                                            evaluateInitiativeBudget(reward, initiativeConfig, initiativeCounter, ruleEngineResult);
                                            final Long previousRewards = ruleEngineResult.getRefundInfo() != null ? Optional.ofNullable(ruleEngineResult.getRefundInfo().getPreviousRewards().get(initiativeId)).map(RefundInfo.PreviousReward::getAccruedRewardCents).orElse(null) : null;
                                            initiativeCounter.setVersion(initiativeCounter.getVersion()+1L);
                                            initiativeCounter.setUpdateDate(LocalDateTime.now());
                                            updateCounters(initiativeCounter, ruleEngineResult.getOperationTypeTranscoded(), reward, previousRewards, ruleEngineResult.getAmountCents(), ruleEngineResult.getEffectiveAmountCents(), justTrxCountRejection);
                                            updateTemporalCounters(initiativeCounter, ruleEngineResult.getOperationTypeTranscoded(), reward, ruleEngineResult, previousRewards, initiativeConfig, justTrxCountRejection);
                                            updateLastTrxCounters(initiativeCounter, ruleEngineResult);
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
        if(reward.getAccruedRewardCents().compareTo(ruleEngineResult.getEffectiveAmountCents() )> 0){
            log.warn("[REWARD] The following initiative calculated a reward greater than the amount: initiativeId={}, amount={}, reward={}, trxId={}",
                    initiativeId, ruleEngineResult.getEffectiveAmountCents(), reward.getAccruedRewardCents(), ruleEngineResult.getId());

            reward.setAccruedRewardCents(ruleEngineResult.getEffectiveAmountCents());
        }
    }

    private void evaluateInitiativeBudget(Reward reward, InitiativeConfig initiativeConfig, UserInitiativeCounters initiativeCounter, RewardTransactionDTO trx) {
        Long budgetCents = trx.getVoucherAmountCents() != null ? trx.getVoucherAmountCents() : initiativeConfig.getBeneficiaryBudgetCents();
        initiativeCounter.setExhaustedBudget(budgetCents != null && ((initiativeCounter.getTotalRewardCents() + reward.getAccruedRewardCents())>=(budgetCents)));
        if (initiativeCounter.isExhaustedBudget()) {
            Long newAccruedRewardCents = budgetCents - (initiativeCounter.getTotalRewardCents());
            reward.setCapped(newAccruedRewardCents.compareTo(reward.getAccruedRewardCents()) != 0);
            reward.setAccruedRewardCents(newAccruedRewardCents);
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
        return initiativeReward != null && initiativeReward.getAccruedRewardCents().compareTo(0L) != 0;
    }

    private boolean isJustTrxCountRejection(RewardTransactionDTO ruleEngineResult, String initiativeId) {
        return isJustTrxCountRejection(ruleEngineResult.getInitiativeRejectionReasons().get(initiativeId));
    }

    private boolean isJustTrxCountRejection(List<String> ruleEngineResult) {
        return justTrxCountRejectionReason.equals(ruleEngineResult);
    }

    private void updateCounters(Counters counters, OperationType operationType, Reward reward, Long previousRewards, Long amountCents, Long effectiveAmount, boolean justTrxCountRejection) {
        if(justTrxCountRejection){
            handleTrxCountRejection(counters, operationType, effectiveAmount, reward);
        } else {
            if (previousRewards == null) {
                counters.setTrxNumber(counters.getTrxNumber() + 1);
                counters.setTotalAmountCents(counters.getTotalAmountCents() + effectiveAmount);
            } else {
                if (previousRewards.compareTo(0L)>0) {
                    counters.setTotalAmountCents(counters.getTotalAmountCents() - amountCents);
                    // if previous reward was 0, due to CAP, when reverting it, it should not decrease the counters
                    if (Long.compare(0L, previousRewards + reward.getAccruedRewardCents()) == 0) {
                        counters.setTrxNumber(counters.getTrxNumber() - 1);
                        counters.setTotalAmountCents(counters.getTotalAmountCents() - effectiveAmount);
                        reward.setCompleteRefund(true);
                    }
                }
            }
            counters.setTotalRewardCents(counters.getTotalRewardCents() + (reward.getAccruedRewardCents()));
        }
    }

    private static void handleTrxCountRejection(Counters counters, OperationType operationType, Long effectiveAmountCents, Reward reward) {
        // charge op
        if (OperationType.CHARGE.equals(operationType)) {
            counters.setTrxNumber(counters.getTrxNumber() + 1);
        } else {
            // complete refund
            if (0L == effectiveAmountCents) {
                counters.setTrxNumber(counters.getTrxNumber() - 1);
                reward.setCompleteRefund(true);
            }
        }
    }

    private void updateTemporalCounters(UserInitiativeCounters userInitiativeCounters, OperationType operationType, Reward initiativeReward, RewardTransactionDTO ruleEngineResult, Long previousRewardsCents, InitiativeConfig initiativeConfig, boolean justTrxCountRejection) {
        if (initiativeConfig.isDailyThreshold()) {
            updateTemporalCounter(userInitiativeCounters.getDailyCounters(), RewardConstants.dayDateFormatter, ruleEngineResult, operationType, previousRewardsCents, initiativeReward, justTrxCountRejection);
        }
        if (initiativeConfig.isWeeklyThreshold()) {
            updateTemporalCounter(userInitiativeCounters.getWeeklyCounters(), RewardConstants.weekDateFormatter, ruleEngineResult, operationType, previousRewardsCents, initiativeReward, justTrxCountRejection);
        }
        if (initiativeConfig.isMonthlyThreshold()) {
            updateTemporalCounter(userInitiativeCounters.getMonthlyCounters(), RewardConstants.monthDateFormatter, ruleEngineResult, operationType, previousRewardsCents, initiativeReward, justTrxCountRejection);
        }
        if (initiativeConfig.isYearlyThreshold()) {
            updateTemporalCounter(userInitiativeCounters.getYearlyCounters(), RewardConstants.yearDateFormatter, ruleEngineResult, operationType, previousRewardsCents, initiativeReward, justTrxCountRejection);
        }
    }

    private void updateTemporalCounter(Map<String, Counters> periodicalMap, DateTimeFormatter periodicalKeyFormatter, RewardTransactionDTO ruleEngineResult, OperationType operationType, Long previousRewardsCents, Reward initiativeReward, boolean justTrxCountRejection) {
        updateCounters(periodicalMap.computeIfAbsent(periodicalKeyFormatter.format(ruleEngineResult.getTrxChargeDate()), k -> new Counters()), operationType, initiativeReward, previousRewardsCents, ruleEngineResult.getAmountCents(), ruleEngineResult.getEffectiveAmountCents(), justTrxCountRejection);
    }

    private void updateLastTrxCounters(UserInitiativeCounters initiativeCounter, RewardTransactionDTO ruleEngineResult) {
        LocalDateTime expiredTime = LocalDateTime.now().minus(lastTrxExpired);

        //delete transactions expired
        List<LastTrxInfoDTO> listUpdated = initiativeCounter.getLastTrx()
                .stream()
                .filter(trx -> trx.getElaborationDateTime().isAfter(expiredTime))
                .collect(Collectors.toList());

        //add new processed transaction
        listUpdated.add(baseTransactionProcessed2LastTrxInfoDTOMapper.apply(ruleEngineResult));

        initiativeCounter.setLastTrx(listUpdated);
    }

}
