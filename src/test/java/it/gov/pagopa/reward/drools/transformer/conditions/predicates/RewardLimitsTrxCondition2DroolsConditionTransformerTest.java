package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

class RewardLimitsTrxCondition2DroolsConditionTransformerTest extends InitiativeTrxCondition2DroolsConditionTransformerTest {

    private static final LocalDateTime TRX_DATE = LocalDateTime.of(LocalDate.of(2022, 1, 8), LocalTime.NOON);
    private final String initiativeId = "RewardLimit";
    private final RewardLimitsTrxCondition2DroolsConditionTransformer transformer = new RewardLimitsTrxCondition2DroolsConditionTransformer();

    private final BigDecimal rewardLimit = BigDecimal.valueOf(15);

    private BigDecimal totalReward;

    @Override
    protected UserInitiativeCounters getInitiativeCounters() {
        final UserInitiativeCounters counter = new UserInitiativeCounters("USERID", InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId);
        counter.setDailyCounters(new HashMap<>(Map.of(
            "2022-01-08", Counters.builder().totalReward(totalReward).build()
        )));
        counter.setWeeklyCounters(new HashMap<>(Map.of(
                "2022-01-1", Counters.builder().totalReward(totalReward).build()
        )));
        counter.setMonthlyCounters(new HashMap<>(Map.of(
                "2022-01", Counters.builder().totalReward(totalReward).build()
        )));
        counter.setYearlyCounters(new HashMap<>(Map.of(
                "2022", Counters.builder().totalReward(totalReward).build()
        )));

        return counter;
    }

    private void testRewardLimit(String rewardLimitsCondition, TransactionDroolsDTO transaction) {
        transaction.setTrxChargeDate(OffsetDateTime.of(TRX_DATE, CommonConstants.ZONEID.getRules().getOffset(TRX_DATE)));
        transaction.setRewards(new HashMap<>(Map.of(
                initiativeId, new Reward(initiativeId, "ORGANIZATION_"+initiativeId, BigDecimal.valueOf(10).setScale(2, RoundingMode.UNNECESSARY))
        )));

        totalReward = rewardLimit.subtract(BigDecimal.ONE);
        testRule(initiativeId, rewardLimitsCondition, transaction, true);

        totalReward = rewardLimit;
        testRule(initiativeId, rewardLimitsCondition, transaction, false);

        totalReward = rewardLimit.add(BigDecimal.ONE);
        testRule(initiativeId, rewardLimitsCondition, transaction, false);
    }

    @Test
    void testDailyLimit() {
        RewardLimitsDTO initiativeTrxCondition = new RewardLimitsDTO();
        initiativeTrxCondition.setFrequency(RewardLimitsDTO.RewardLimitFrequency.DAILY);
        initiativeTrxCondition.setRewardLimit(rewardLimit);
        String thresholdCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("(it.gov.pagopa.reward.enums.OperationType.valueOf(\"REFUND\").equals($trx.getOperationTypeTranscoded()) || $userInitiativeCounters.getDailyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getDayDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal(\"15\")) < 0)", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testRewardLimit(thresholdCondition, transaction);
    }

    @Test
    void testWeeklyLimit() {
        RewardLimitsDTO initiativeTrxCondition = new RewardLimitsDTO();
        initiativeTrxCondition.setFrequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY);
        initiativeTrxCondition.setRewardLimit(rewardLimit);
        String trxCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("(it.gov.pagopa.reward.enums.OperationType.valueOf(\"REFUND\").equals($trx.getOperationTypeTranscoded()) || $userInitiativeCounters.getWeeklyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getWeekDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal(\"15\")) < 0)", trxCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testRewardLimit(trxCondition, transaction);
    }

    @Test
    void testMonthlyLimit() {
        RewardLimitsDTO initiativeTrxCondition = new RewardLimitsDTO();
        initiativeTrxCondition.setFrequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY);
        initiativeTrxCondition.setRewardLimit(rewardLimit);
        String thresholdCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("(it.gov.pagopa.reward.enums.OperationType.valueOf(\"REFUND\").equals($trx.getOperationTypeTranscoded()) || $userInitiativeCounters.getMonthlyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getMonthDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal(\"15\")) < 0)", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testRewardLimit(thresholdCondition, transaction);
    }

    @Test
    void testYearlyLimit() {
        RewardLimitsDTO initiativeTrxCondition = new RewardLimitsDTO();
        initiativeTrxCondition.setFrequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY);
        initiativeTrxCondition.setRewardLimit(rewardLimit);
        String thresholdCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("(it.gov.pagopa.reward.enums.OperationType.valueOf(\"REFUND\").equals($trx.getOperationTypeTranscoded()) || $userInitiativeCounters.getYearlyCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.getYearDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal(\"15\")) < 0)", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testRewardLimit(thresholdCondition, transaction);
    }


}
