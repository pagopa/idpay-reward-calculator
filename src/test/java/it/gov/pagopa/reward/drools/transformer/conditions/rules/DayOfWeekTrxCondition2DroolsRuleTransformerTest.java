package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.trx.DayOfWeekDTO;
import it.gov.pagopa.reward.dto.rule.trx.DayOfWeekDTOFaker;
import it.gov.pagopa.reward.model.RewardTransaction;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.time.*;

public class DayOfWeekTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<DayOfWeekDTO> {

    private final DayOfWeekTrxCondition2DroolsRuleTransformer transformer = new DayOfWeekTrxCondition2DroolsRuleTransformer(new TrxCondition2DroolsConditionTransformerFacadeImpl());
    public final DayOfWeekDTO dayOfWeekRule = DayOfWeekDTOFaker.mockInstance(1);

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
                salience 3
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(((trxDate.dayOfWeek in (java.time.DayOfWeek.valueOf("WEDNESDAY")) && ((trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() >= java.time.LocalTime.of(0,45,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() <= java.time.LocalTime.of(2,0,0,0)))) || (trxDate.dayOfWeek in (java.time.DayOfWeek.valueOf("WEDNESDAY")) && ((trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() >= java.time.LocalTime.of(0,45,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() <= java.time.LocalTime.of(2,0,0,0)) || (trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() >= java.time.LocalTime.of(7,45,0,0) && trxDate.atZoneSameInstant(java.time.ZoneId.of("Europe/Rome")).toLocalTime() <= java.time.LocalTime.of(9,0,0,0)))))))
                then $trx.getRejectionReason().add("TRX_RULE_DAYOFWEEK_FAIL");
                end
                """;
    }

    @Override
    protected RewardTransaction getSuccessfulUseCase() {
        RewardTransaction trx = new RewardTransaction();
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 5), LocalTime.of(0, 45));
        trx.setTrxDate(OffsetDateTime.of(localDateTime, ZoneId.of("Europe/Rome").getRules().getOffset(localDateTime)));
        return trx;
    }

    @Override
    protected RewardTransaction getFailingUseCase() {
        RewardTransaction trx = new RewardTransaction();
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 5), LocalTime.of(0, 44));
        trx.setTrxDate(OffsetDateTime.of(localDateTime, ZoneId.of("Europe/Rome").getRules().getOffset(localDateTime)));
        return trx;
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.DAYOFWEEK.getRejectionReason();
    }
}
