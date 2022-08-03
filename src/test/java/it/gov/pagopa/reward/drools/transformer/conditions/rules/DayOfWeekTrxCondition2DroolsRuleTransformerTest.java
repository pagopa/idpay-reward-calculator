package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.trx.DayOfWeekDTO;
import it.gov.pagopa.reward.test.fakers.rule.DayOfWeekDTOFaker;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.time.*;
import java.util.List;
import java.util.function.Supplier;

class DayOfWeekTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<DayOfWeekDTO> {

    private final DayOfWeekTrxCondition2DroolsRuleTransformer transformer = new DayOfWeekTrxCondition2DroolsRuleTransformer(new TrxCondition2DroolsConditionTransformerFacadeImpl());
    private final DayOfWeekDTO dayOfWeekRule = DayOfWeekDTOFaker.mockInstance(1);

    @Override
    protected DayOfWeekTrxCondition2DroolsRuleTransformer getTransformer() {
        return transformer;
    }

    @Override
    protected DayOfWeekDTO getInitiativeTrxCondition() {
        return dayOfWeekRule;
    }

    @Override
    protected String getExpectedRule() {
        return """
                                
                rule "ruleName-DAYOFWEEK"
                salience 1
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("agendaGroup", new it.gov.pagopa.reward.model.counters.InitiativeCounters())
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("agendaGroup") == null, !(((trxDate.dayOfWeek in (java.time.DayOfWeek.valueOf("WEDNESDAY")) && ((trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() >= java.time.LocalTime.of(0,45,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() <= java.time.LocalTime.of(2,0,0,0)))) || (trxDate.dayOfWeek in (java.time.DayOfWeek.valueOf("WEDNESDAY")) && ((trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() >= java.time.LocalTime.of(0,45,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() <= java.time.LocalTime.of(2,0,0,0)) || (trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() >= java.time.LocalTime.of(7,45,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() <= java.time.LocalTime.of(9,0,0,0)))))))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("agendaGroup",k->new java.util.ArrayList<>()).add("TRX_RULE_DAYOFWEEK_FAIL");
                end
                """;
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getSuccessfulUseCaseSuppliers() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 5), LocalTime.of(0, 45));
        trx.setTrxDate(OffsetDateTime.of(localDateTime, ZoneId.of("Europe/Rome").getRules().getOffset(localDateTime)));
        return List.of(() -> trx);
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getFailingUseCaseSuppliers() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 5), LocalTime.of(0, 44));
        trx.setTrxDate(OffsetDateTime.of(localDateTime, ZoneId.of("Europe/Rome").getRules().getOffset(localDateTime)));
        return List.of(() -> trx);
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.DAYOFWEEK.getRejectionReason();
    }
}
