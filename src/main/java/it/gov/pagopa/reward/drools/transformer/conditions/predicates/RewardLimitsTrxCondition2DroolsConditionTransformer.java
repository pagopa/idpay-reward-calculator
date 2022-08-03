package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl;
import org.springframework.data.util.Pair;

import java.util.Map;

public class RewardLimitsTrxCondition2DroolsConditionTransformer implements InitiativeTrxCondition2DroolsConditionTransformer<RewardLimitsDTO> {

    private static final Map<RewardLimitsDTO.RewardLimitFrequency, Pair<String, String>> rewardLimitFrequency2CountersFieldAndFormatter = Map.of(
            RewardLimitsDTO.RewardLimitFrequency.DAILY, Pair.of("getDailyCounters()","getDayDateFormatter()"),
            RewardLimitsDTO.RewardLimitFrequency.WEEKLY, Pair.of("getWeeklyCounters()","getWeekDateFormatter()"),
            RewardLimitsDTO.RewardLimitFrequency.MONTHLY, Pair.of("getMonthlyCounters()","getMonthDateFormatter()"),
            RewardLimitsDTO.RewardLimitFrequency.YEARLY, Pair.of("getYearlyCounters()","getYearDateFormatter()")
    );

    public static Pair<String, String> getRewardLimitFrequencyConfig(RewardLimitsDTO.RewardLimitFrequency frequency){
        final Pair<String, String> frequencyConfig = rewardLimitFrequency2CountersFieldAndFormatter.get(frequency);
        if(frequencyConfig==null){
            throw new IllegalStateException("Unhandled frequency %s".formatted(frequency));
        }
        return frequencyConfig;
    }

    @Override
    public String apply(String initiativeId, RewardLimitsDTO rewardLimitsDTO) {
        return "%s.totalReward.compareTo(%s) < 0".formatted(
                buildFrequencyCounterExpression(rewardLimitsDTO.getFrequency()),
                DroolsTemplateRuleUtils.toTemplateParam(rewardLimitsDTO.getRewardLimit())
        );
    }

    public static String buildFrequencyCounterExpression(RewardLimitsDTO.RewardLimitFrequency frequency) {
        final Pair<String, String> frequencyConfig = getRewardLimitFrequencyConfig(frequency);
        // using constructor with parameters when creating Counters because drools give a warning when using a constructor without parameters (due to a bug on Drools https://issues.redhat.com/browse/DROOLS-7095)
        return "$initiativeCounters.%s.getOrDefault(%s.%s.format($trx.getTrxDate()), new %s(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO))".formatted(
                frequencyConfig.getFirst(),
                UserInitiativeCountersUpdateServiceImpl.class.getName(),
                frequencyConfig.getSecond(),
                Counters.class.getName()
        );
    }
}
