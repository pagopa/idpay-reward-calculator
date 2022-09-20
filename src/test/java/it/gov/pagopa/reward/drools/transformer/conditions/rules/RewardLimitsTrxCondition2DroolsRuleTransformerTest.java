package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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
        trx.setTrxDate(OffsetDateTime.of(TRX_DATE, RewardConstants.ZONEID.getRules().getOffset(TRX_DATE)));
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
                salience 6
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("agendaGroup", new it.gov.pagopa.reward.model.counters.InitiativeCounters("agendaGroup"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("agendaGroup") == null, !($initiativeCounters.get%sCounters().getOrDefault(it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateServiceImpl.get%sDateFormatter().format($trx.getTrxDate()), new it.gov.pagopa.reward.model.counters.Counters(0L, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)).totalReward.compareTo(new java.math.BigDecimal("10")) < 0))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("agendaGroup",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDLIMITS_%s_FAIL");
                end
                """.formatted(
                rewardLimitRule.getFrequency().name(),
                StringUtils.capitalize(rewardLimitRule.getFrequency().name().toLowerCase()),
                rewardLimitRule.getFrequency() == RewardLimitsDTO.RewardLimitFrequency.DAILY ? "Day" : StringUtils.capitalize(rewardLimitRule.getFrequency().name().toLowerCase()).replace("ly", ""),
                rewardLimitRule.getFrequency().name()
        );
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getSuccessfulUseCaseSuppliers() {
        return List.of(
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.DAILY);
                    counters = new InitiativeCounters("agendaGroup");
                    counters.setDailyCounters(new HashMap<>(Map.of(
                            "2022-03-15", new Counters(0L, BigDecimal.ZERO, BigDecimal.ZERO)
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY);
                    counters = new InitiativeCounters("agendaGroup");
                    counters.setWeeklyCounters(new HashMap<>(Map.of(
                            "2022-03-3", new Counters(1L, BigDecimal.valueOf(5), BigDecimal.valueOf(5))
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY);
                    counters = new InitiativeCounters("agendaGroup");
                    counters.setMonthlyCounters(new HashMap<>(Map.of(
                            "2022-03", new Counters(1L, BigDecimal.valueOf(9), BigDecimal.valueOf(5))
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY);
                    counters = new InitiativeCounters("agendaGroup");
                    useCase="";
                    return buildTrx();
                }
        );
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getFailingUseCaseSuppliers() {
        return List.of(
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.DAILY);
                    counters = new InitiativeCounters("agendaGroup");
                    counters.setDailyCounters(new HashMap<>(Map.of(
                            "2022-03-15", new Counters(0L, BigDecimal.TEN, BigDecimal.ZERO)
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY);
                    counters = new InitiativeCounters("agendaGroup");
                    counters.setWeeklyCounters(new HashMap<>(Map.of(
                            "2022-03-3", new Counters(1L, BigDecimal.valueOf(10), BigDecimal.ONE)
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY);
                    counters = new InitiativeCounters("agendaGroup");
                    counters.setMonthlyCounters(new HashMap<>(Map.of(
                            "2022-03", new Counters(1L, BigDecimal.valueOf(11), BigDecimal.valueOf(5))
                    )));
                    useCase="";
                    return buildTrx();
                },
                () -> {
                    rewardLimitRule.setFrequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY);
                    counters = new InitiativeCounters("agendaGroup");
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
