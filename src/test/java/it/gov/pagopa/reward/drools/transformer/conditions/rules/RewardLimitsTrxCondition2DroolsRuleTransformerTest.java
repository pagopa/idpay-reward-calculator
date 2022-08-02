package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

class RewardLimitsTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<RewardLimitsDTO> {

    private static final LocalDateTime TRX_DATE = LocalDateTime.of(LocalDate.of(2022, 3, 15), LocalTime.NOON);

    private final RewardLimitsTrxCondition2DroolsRuleTransformer transformer = new RewardLimitsTrxCondition2DroolsRuleTransformer(new TrxCondition2DroolsConditionTransformerFacadeImpl());
    private InitiativeCounters counters;
    private String useCase;
    private final RewardLimitsDTO rewardLimitRule = RewardLimitsDTO.builder()
            .frequency(RewardLimitsDTO.RewardLimitFrequency.DAILY)
            .rewardLimit(BigDecimal.TEN)
            .build();

    private TransactionDroolsDTO buildTrx() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setTrxDate(OffsetDateTime.of(TRX_DATE, ZoneId.of("Europe/Rome").getRules().getOffset(TRX_DATE)));
        trx.setRewards(new HashMap<>(Map.of(
                "agendaGroup", new Reward(BigDecimal.valueOf(10).setScale(2, RoundingMode.UNNECESSARY))
        )));
        return trx;
    }

    @Override
    protected RewardLimitsTrxCondition2DroolsRuleTransformer getTransformer() {
        return transformer;
    }

    @Override
    protected RewardLimitsDTO getInitiativeTrxCondition() {
        return rewardLimitRule;
    }

    @Override
    protected String getExpectedRule() {
        return """
                                
                rule "ruleName-%s-REWARDLIMITS"
                salience 8
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("agendaGroup", new it.gov.pagopa.reward.model.counters.InitiativeCounters())
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("agendaGroup") == null, !((rewards.get("agendaGroup") == null || $initiativeCounters.get%sCounters().getOrDefault(it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateServiceImpl.get%sDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters()).totalReward.compareTo(new java.math.BigDecimal("10")) < 0)))
                then $trx.getRewards().remove("agendaGroup");
                   $trx.getInitiativeRejectionReasons().computeIfAbsent("agendaGroup",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDLIMITS_%s_FAIL");
                end
                """.formatted(
                rewardLimitRule.getFrequency().name(),
                StringUtils.capitalize(rewardLimitRule.getFrequency().name().toLowerCase()),
                rewardLimitRule.getFrequency() == RewardLimitsDTO.RewardLimitFrequency.DAILY ? "Day" : StringUtils.capitalize(rewardLimitRule.getFrequency().name().toLowerCase()).replace("ly", ""),
                rewardLimitRule.getFrequency().name()
        );
    }

    @Override
    protected TransactionDroolsDTO testRule(String rule, Supplier<TransactionDroolsDTO> trxSupplier, boolean simulateOtherRejection, boolean expectRejectionReason, boolean shortCircuited) {
        final TransactionDroolsDTO result = super.testRule(rule, trxSupplier, simulateOtherRejection, expectRejectionReason, shortCircuited);
        if(!shortCircuited && expectRejectionReason){
            Assertions.assertNull(result.getRewards().get("agendaGroup"));
        }
        return result;
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getSuccessfulUseCaseSuppliers() {
        return List.of(
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.DAILY);
                    counters = new InitiativeCounters();
                    counters.setDailyCounters(new HashMap<>(Map.of(
                            "2022-03-15", new Counters(0L, BigDecimal.ZERO, BigDecimal.ZERO)
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY);
                    counters = new InitiativeCounters();
                    counters.setWeeklyCounters(new HashMap<>(Map.of(
                            "2022-03-3", new Counters(1L, BigDecimal.valueOf(5), BigDecimal.valueOf(5))
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY);
                    counters = new InitiativeCounters();
                    counters.setMonthlyCounters(new HashMap<>(Map.of(
                            "2022-03", new Counters(1L, BigDecimal.valueOf(9), BigDecimal.valueOf(5))
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY);
                    counters = new InitiativeCounters();
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY);
                    counters = new InitiativeCounters();
                    final TransactionDroolsDTO trx = buildTrx();
                    trx.setRewards(Collections.emptyMap());
                    useCase="No Reward";
                    return trx;
                }
        );
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getFailingUseCaseSuppliers() {
        return List.of(
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.DAILY);
                    counters = new InitiativeCounters();
                    counters.setDailyCounters(new HashMap<>(Map.of(
                            "2022-03-15", new Counters(0L, BigDecimal.TEN, BigDecimal.ZERO)
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY);
                    counters = new InitiativeCounters();
                    counters.setWeeklyCounters(new HashMap<>(Map.of(
                            "2022-03-3", new Counters(1L, BigDecimal.valueOf(10), BigDecimal.ONE)
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY);
                    counters = new InitiativeCounters();
                    counters.setMonthlyCounters(new HashMap<>(Map.of(
                            "2022-03", new Counters(1L, BigDecimal.valueOf(11), BigDecimal.valueOf(5))
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY);
                    counters = new InitiativeCounters();
                    counters.setYearlyCounters(new HashMap<>(Map.of(
                            "2022", new Counters(1L, BigDecimal.valueOf(12), BigDecimal.ONE)
                    )));
                    useCase="";
                    return buildTrx();
                }
        );
    }

    @Override
    protected InitiativeCounters getInitiativeCounters() {
        return counters;
    }

    @Override
    protected String toUseCase(TransactionDroolsDTO trx) {
        return "frequency: %s %s".formatted(rewardLimitRule.getFrequency(), useCase);
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.REWARDLIMITS.getRejectionReason().formatted(rewardLimitRule.getFrequency().name());
    }
}
