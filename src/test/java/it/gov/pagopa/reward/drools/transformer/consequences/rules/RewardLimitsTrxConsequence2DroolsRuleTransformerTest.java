package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class RewardLimitsTrxConsequence2DroolsRuleTransformerTest extends InitiativeTrxConsequence2DroolsRuleTransformerTest<RewardLimitsDTO> {

    private final BigDecimal PREVIOUS_REWARD = toExpectedScale(BigDecimal.ONE);

    private final RewardLimitsTrxConsequence2DroolsRuleTransformer transformer = new RewardLimitsTrxConsequence2DroolsRuleTransformer(new TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl());

    private final RewardLimitsDTO rewardLimitsDTO = new RewardLimitsDTO();

    @Override
    protected InitiativeTrxConsequence2DroolsRuleTransformer<RewardLimitsDTO> getTransformer() {
        return transformer;
    }

    @Override
    protected RewardLimitsDTO getInitiativeTrxConsequence() {
        return rewardLimitsDTO;
    }

    private enum USECASES {
        DAILY_NO_CAP,
        DAILY_CAP,
        MONTHLY_NO_CAP,
        WEEKLY_CAP,
        WEEKLY_NO_CAP,
        MONTHLY_CAP,
        YEARLY_NO_CAP,
        YEARLY_CAP,
        NO_COUNTER,
        DISCARDED,
        REFUNDED_NO_PREVIOUS,
        REFUNDED_WITH_PREVIOUS
    }

    public static final BigDecimal TRANSACTION_REWARD = BigDecimal.TEN;
    public static final BigDecimal TOTAL_REWARD = BigDecimal.valueOf(8);

    private USECASES useCase;
    private BigDecimal expectedReward;

    private void configureUseCase(USECASES useCase){
        this.useCase=useCase;
        switch (useCase){
            case DISCARDED, NO_COUNTER -> {
                rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.DAILY);
                rewardLimitsDTO.setRewardLimit(TRANSACTION_REWARD);
                expectedReward=toExpectedScale(TRANSACTION_REWARD);
            }
            case DAILY_CAP -> {
                rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.DAILY);
                rewardLimitsDTO.setRewardLimit(TRANSACTION_REWARD);
                expectedReward=bigDecimalValue(2);
            }
            case DAILY_NO_CAP -> {
                rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.DAILY);
                rewardLimitsDTO.setRewardLimit(BigDecimal.valueOf(18.01));
                expectedReward=toExpectedScale(TRANSACTION_REWARD);
            }
            case WEEKLY_CAP -> {
                rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY);
                rewardLimitsDTO.setRewardLimit(BigDecimal.valueOf(17.99));
                expectedReward=bigDecimalValue(9.99);
            }
            case WEEKLY_NO_CAP -> {
                rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY);
                rewardLimitsDTO.setRewardLimit(BigDecimal.valueOf(18));
                expectedReward=toExpectedScale(TRANSACTION_REWARD);
            }
            case MONTHLY_CAP -> {
                rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY);
                rewardLimitsDTO.setRewardLimit(BigDecimal.ONE);
                expectedReward=bigDecimalValue(0);
            }
            case MONTHLY_NO_CAP -> {
                rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY);
                rewardLimitsDTO.setRewardLimit(BigDecimal.valueOf(19));
                expectedReward=toExpectedScale(TRANSACTION_REWARD);
            }
            case YEARLY_CAP, REFUNDED_NO_PREVIOUS -> {
                rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY);
                rewardLimitsDTO.setRewardLimit(BigDecimal.ZERO);
                expectedReward=bigDecimalValue(0);
            }
            case REFUNDED_WITH_PREVIOUS -> {
                rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY);
                rewardLimitsDTO.setRewardLimit(BigDecimal.ZERO);
                expectedReward=PREVIOUS_REWARD;
            }
            case YEARLY_NO_CAP -> {
                rewardLimitsDTO.setFrequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY);
                rewardLimitsDTO.setRewardLimit(BigDecimal.valueOf(100));
                expectedReward=toExpectedScale(TRANSACTION_REWARD);
            }
        }
    }

    @Override
    public BigDecimal getExpectedReward() {
        return expectedReward;
    }

    @Override
    protected String getExpectedRule() {
        return """
                                
                rule "ruleName-%s-REWARDLIMITS-CAP"
                salience -2
                agenda-group "initiativeId"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("initiativeId", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "initiativeId"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("initiativeId") == null)
                then\s
                   it.gov.pagopa.reward.dto.trx.Reward reward = $trx.getRewards().get("initiativeId");
                   if(reward != null){
                      java.math.BigDecimal oldAccruedReward=reward.getAccruedReward();
                      reward.setAccruedReward($trx.getRewards().get("initiativeId").getAccruedReward().min(java.math.BigDecimal.ZERO.max(new java.math.BigDecimal("%s").subtract($userInitiativeCounters.get%sCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.get%sDateFormatter().format($trx.getTrxChargeDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).getTotalReward().subtract($trx.getRefundInfo()!=null && $trx.getRefundInfo().getPreviousRewards()!=null && $trx.getRefundInfo().getPreviousRewards().get("initiativeId")!=null ? $trx.getRefundInfo().getPreviousRewards().get("initiativeId").getAccruedReward() : java.math.BigDecimal.ZERO)))).setScale(2, java.math.RoundingMode.HALF_DOWN));
                      if(reward.getAccruedReward().compareTo(oldAccruedReward) != 0){
                         reward.set%sCapped(true);
                         %s
                      }
                   }
                end
                """.formatted(
                rewardLimitsDTO.getFrequency().name(),
                rewardLimitsDTO.getRewardLimit(),
                StringUtils.capitalize(rewardLimitsDTO.getFrequency().name().toLowerCase()),
                rewardLimitsDTO.getFrequency() == RewardLimitsDTO.RewardLimitFrequency.DAILY ? "Day" : StringUtils.capitalize(rewardLimitsDTO.getFrequency().name().toLowerCase()).replace("ly", ""),
                StringUtils.capitalize(rewardLimitsDTO.getFrequency().name().toLowerCase()),
                Arrays.stream(RewardLimitsDTO.RewardLimitFrequency.values())
                        .filter(f->!f.equals(rewardLimitsDTO.getFrequency()))
                        .map(f->"reward.set%sCapped(false);".formatted(StringUtils.capitalize(f.name().toLowerCase())))
                        .collect(Collectors.joining("\n         "))
                );
    }

    @Override
    protected TransactionDroolsDTO getTransaction() {
        final TransactionDroolsDTO trx = new TransactionDroolsDTO();
        LocalDateTime trxDateTime = LocalDateTime.of(LocalDate.of(2022, 3, 15), LocalTime.NOON);
        trx.setTrxChargeDate(OffsetDateTime.of(trxDateTime, CommonConstants.ZONEID.getRules().getOffset(trxDateTime)));
        if(this.useCase.equals(USECASES.REFUNDED_NO_PREVIOUS) || this.useCase.equals(USECASES.REFUNDED_WITH_PREVIOUS)){
            trx.setOperationTypeTranscoded(OperationType.REFUND);
            if(this.useCase.equals(USECASES.REFUNDED_WITH_PREVIOUS)){
                trx.setRefundInfo(new RefundInfo());
                trx.getRefundInfo().setPreviousRewards(Map.of("initiativeId", new RefundInfo.PreviousReward("initiativeId", "organizationId", PREVIOUS_REWARD)));
            }
        }
        return trx;
    }


    @Override
    protected void cleanRewards(TransactionDroolsDTO trx) {
        super.cleanRewards(trx);
        if (!useCase.equals(USECASES.DISCARDED)) {
            trx.getRewards().put("initiativeId", new Reward("initiativeId", "organizationId", TRANSACTION_REWARD));
        }
    }

    @Override
    protected UserInitiativeCountersWrapper getCounters() {
        if (USECASES.NO_COUNTER.equals(useCase)) {
            return null;
        } else {
            UserInitiativeCountersWrapper counters = new UserInitiativeCountersWrapper("userId", new HashMap<>());
            counters.getInitiatives().put("initiativeId", UserInitiativeCounters.builder("userId", InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,"initiativeId")
                    .dailyCounters(
                            new HashMap<>(Map.of(
                                    "2022-03-15", Counters.builder()
                                            .totalReward(TOTAL_REWARD)
                                            .build()))
                    )
                    .weeklyCounters(
                            new HashMap<>(Map.of(
                                    "2022-03-3", Counters.builder()
                                            .totalReward(TOTAL_REWARD)
                                            .build()))
                    )
                    .monthlyCounters(
                            new HashMap<>(Map.of(
                                    "2022-03", Counters.builder()
                                            .totalReward(TOTAL_REWARD)
                                            .build()))
                    )
                    .build()
            );
            return counters;
        }
    }

    @Test
    @Override
    void testDiscardedIfRejected() {
        configureUseCase(USECASES.DISCARDED);
        super.testDiscardedIfRejected();
    }

    @Test
    void testRewardCounterNotInitiated() {
        configureUseCase(USECASES.NO_COUNTER);
        super.testReward();
    }

    // test suppressed in order to execute it using a different name
    @Override
    void testReward() {
        super.testReward();
    }

    @Test
    void testRewardDailyCapped(){
        configureUseCase(USECASES.DAILY_CAP);
        testReward();
    }

    @Test
    void testRewardDailyNoCapped(){
        configureUseCase(USECASES.DAILY_NO_CAP);
        testReward();
    }

    @Test
    void testRewardWeeklyCapped() {
        configureUseCase(USECASES.WEEKLY_CAP);
        super.testReward();
    }

    @Test
    void testRewardWeeklyNoCapped_perfectMatchLimit() {
        configureUseCase(USECASES.WEEKLY_NO_CAP);
        super.testReward();
    }

    @Test
    void testRewardMontlyCapped_overflow() {
        configureUseCase(USECASES.MONTHLY_CAP);
        super.testReward();
    }

    @Test
    void testRewardMontlyNoCapped() {
        configureUseCase(USECASES.MONTHLY_CAP);
        super.testReward();
    }

    @Test
    void testRewardYearlyCapped() {
        configureUseCase(USECASES.YEARLY_CAP);
        super.testReward();
    }

    @Test
    void testRewardYearlyNoCapped() {
        configureUseCase(USECASES.YEARLY_CAP);
        super.testReward();
    }

    @Test
    void testRefundedNoPrevious() {
        configureUseCase(USECASES.REFUNDED_NO_PREVIOUS);
        super.testReward();
    }

    @Test
    void testRefundedWithPrevious() {
        configureUseCase(USECASES.REFUNDED_WITH_PREVIOUS);
        super.testReward();
    }

    @Override
    protected TransactionDroolsDTO testRule(String rule, TransactionDroolsDTO trx, BigDecimal expectReward) {
        super.testRule(rule, trx, expectReward);
        if(!useCase.equals(USECASES.DISCARDED)){
            boolean expectedDailyCapped = useCase.name().contains("DAILY") && !useCase.name().contains("NO_CAP");
            boolean expectedWeeklyCapped = useCase.name().contains("WEEKLY") && !useCase.name().contains("NO_CAP");
            boolean expectedMonthlyCapped = useCase.name().contains("MONTHLY") && !useCase.name().contains("NO_CAP");
            boolean expectedYearlyCapped = (useCase.name().contains("YEARLY") && !useCase.name().contains("NO_CAP") || useCase.equals(USECASES.REFUNDED_NO_PREVIOUS) || useCase.equals(USECASES.REFUNDED_WITH_PREVIOUS));

            assertCaps(trx, expectedDailyCapped, expectedWeeklyCapped, expectedMonthlyCapped, expectedYearlyCapped);
        }
        return trx;
    }

    private void assertCaps(TransactionDroolsDTO trx, boolean expectedDailyCapped, boolean expectedWeeklyCapped, boolean expectedMonthlyCapped, boolean expectedYearlyCapped) {
        final Reward reward = trx.getRewards().get("initiativeId");

        Assertions.assertEquals(expectedDailyCapped, reward.isDailyCapped());
        Assertions.assertEquals(expectedWeeklyCapped, reward.isWeeklyCapped());
        Assertions.assertEquals(expectedMonthlyCapped, reward.isMonthlyCapped());
        Assertions.assertEquals(expectedYearlyCapped, reward.isYearlyCapped());

        Assertions.assertFalse(reward.isCapped());
    }
}
