package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.HashMap;
import java.util.Map;

class RewardLimitsTrxConsequence2DroolsRuleTransformerTest extends InitiativeTrxConsequence2DroolsRuleTransformerTest<RewardLimitsDTO> {

    private final RewardLimitsTrxConsequence2DroolsRuleTransformer transformer = new RewardLimitsTrxConsequence2DroolsRuleTransformer(new TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl());

    // using frequency to indicate the use cases:
    //  DAILY: capped reward
    //  WEEKLY: no capped
    //  MONTHLY: capped to 0
    //  YEARLY: no counter for date
    private final RewardLimitsDTO rewardLimitsDTO = new RewardLimitsDTO();

    @Override
    protected InitiativeTrxConsequence2DroolsRuleTransformer<RewardLimitsDTO> getTransformer() {
        return transformer;
    }

    @Override
    protected RewardLimitsDTO getInitiativeTrxConsequence() {
        return rewardLimitsDTO;
    }

    @Override
    protected String getExpectedRule() {
        return """
                                
                rule "ruleName-%s-REWARDLIMITS"
                salience -3
                agenda-group "agendaGroup"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("agendaGroup", new it.gov.pagopa.reward.model.counters.InitiativeCounters())
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("agendaGroup") == null)
                then\040
                   it.gov.pagopa.reward.dto.Reward reward = $trx.getRewards().get("agendaGroup");
                   if(reward != null){
                      reward.setAccruedReward($trx.getRewards().get("agendaGroup").getAccruedReward().min(java.math.BigDecimal.ZERO.max(new java.math.BigDecimal("%s").subtract($initiativeCounters.get%sCounters().getOrDefault(it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl.get%sDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters()).getTotalReward()))).setScale(2, java.math.RoundingMode.HALF_DOWN));
                      if(reward.getAccruedReward().compareTo(reward.getProvidedReward()) != 0){
                         reward.set%sCapped(true);
                      }
                   }
                end
                """.formatted(
                rewardLimitsDTO.getFrequency().name(),
                rewardLimitsDTO.getRewardLimit(),
                StringUtils.capitalize(rewardLimitsDTO.getFrequency().name().toLowerCase()),
                rewardLimitsDTO.getFrequency() == RewardLimitsDTO.RewardLimitFrequency.DAILY ? "Day" : StringUtils.capitalize(rewardLimitsDTO.getFrequency().name().toLowerCase()).replace("ly", ""),
                StringUtils.capitalize(rewardLimitsDTO.getFrequency().name().toLowerCase())
                );
    }

    @Override
    protected TransactionDroolsDTO getTransaction() {
        final TransactionDroolsDTO trx = new TransactionDroolsDTO();
        LocalDateTime trxDateTime = LocalDateTime.of(LocalDate.of(2022, 3, 15), LocalTime.NOON);
        trx.setTrxDate(OffsetDateTime.of(trxDateTime, ZoneId.of("Europe/Rome").getRules().getOffset(trxDateTime)));
        return trx;
    }

    private boolean testingDiscarded = true;

    @Override
    protected void cleanRewards(TransactionDroolsDTO trx) {
        super.cleanRewards(trx);
        if (!testingDiscarded) {
            trx.getRewards().put("agendaGroup", new Reward(BigDecimal.TEN));
        }
    }

    @Override
    protected UserInitiativeCounters getCounters() {
        if (!rewardLimitsDTO.getFrequency().equals(RewardLimitsDTO.RewardLimitFrequency.YEARLY)) {
            UserInitiativeCounters counters = new UserInitiativeCounters("userId", new HashMap<>());
            counters.getInitiatives().put("agendaGroup", InitiativeCounters.builder()
                    .dailyCounters(
                            new HashMap<>(Map.of(
                                    "2022-03-15", Counters.builder()
                                            .totalReward(BigDecimal.valueOf(8))
                                            .build()))
                    )
                    .weeklyCounters(
                            new HashMap<>(Map.of(
                                    "2022-03-3", Counters.builder()
                                            .totalReward(BigDecimal.valueOf(8))
                                            .build()))
                    )
                    .monthlyCounters(
                            new HashMap<>(Map.of(
                                    "2022-03", Counters.builder()
                                            .totalReward(BigDecimal.valueOf(8))
                                            .build()))
                    )
                    .build()
            );
            return counters;
        } else {
            return null;
        }
    }

    @Override
    protected BigDecimal getExpectedReward() {
        return switch (rewardLimitsDTO.getFrequency()) {
            case DAILY -> bigDecimalValue(2);
            case WEEKLY, YEARLY -> bigDecimalValue(10);
            case MONTHLY -> bigDecimalValue(0);
        };
    }

    @Test
    @Override
    void testDiscardedIfRejected() {
        rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.DAILY);
        rewardLimitsDTO.setRewardLimit(BigDecimal.TEN);

        testingDiscarded = true;
        super.testDiscardedIfRejected();
    }

    @Test
    @Override
    void testReward() {
        rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.DAILY);
        rewardLimitsDTO.setRewardLimit(BigDecimal.TEN);

        testingDiscarded = false;
        super.testReward();
    }

    @Test
    void testRewardNoCapped() {
        rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY);
        rewardLimitsDTO.setRewardLimit(BigDecimal.valueOf(18));

        testingDiscarded = false;
    }

    @Test
    void testRewardOverflow() {
        rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY);
        rewardLimitsDTO.setRewardLimit(BigDecimal.ONE);

        testingDiscarded = false;
    }

    @Test
    void testRewardCounterNotInitiated() {
        rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY);
        rewardLimitsDTO.setRewardLimit(BigDecimal.TEN);

        testingDiscarded = false;
    }

    @Override
    protected TransactionDroolsDTO testRule(String rule, TransactionDroolsDTO trx, BigDecimal expectReward) {
        super.testRule(rule, trx, expectReward);
        if(!testingDiscarded){
            boolean expectedDailyCapped = rewardLimitsDTO.getFrequency().equals(RewardLimitsDTO.RewardLimitFrequency.DAILY);
            boolean expectedWeeklyCapped = false;
            boolean expectedMonthlyCapped = rewardLimitsDTO.getFrequency().equals(RewardLimitsDTO.RewardLimitFrequency.MONTHLY);
            boolean expectedYearlyCapped = false;

            assertCaps(trx, expectedDailyCapped, expectedWeeklyCapped, expectedMonthlyCapped, expectedYearlyCapped);
        }
        return trx;
    }

    private void assertCaps(TransactionDroolsDTO trx, boolean expectedDailyCapped, boolean expectedWeeklyCapped, boolean expectedMonthlyCapped, boolean expectedYearlyCapped) {
        final Reward reward = trx.getRewards().get("agendaGroup");

        Assertions.assertEquals(expectedDailyCapped, reward.isDailyCapped());
        Assertions.assertEquals(expectedWeeklyCapped, reward.isWeeklyCapped());
        Assertions.assertEquals(expectedMonthlyCapped, reward.isMonthlyCapped());
        Assertions.assertEquals(expectedYearlyCapped, reward.isYearlyCapped());

        Assertions.assertFalse(reward.isCapped());
    }
}
