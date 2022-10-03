package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.trx.DayOfWeekDTO;
import org.springframework.util.CollectionUtils;

import java.util.stream.Collectors;

public class DayOfWeekTrxCondition2DroolsConditionTransformer implements InitiativeTrxCondition2DroolsConditionTransformer<DayOfWeekDTO> {

    @Override
    public String apply(String initiativeId, DayOfWeekDTO dayOfWeekTrxCondition) {
        if (!CollectionUtils.isEmpty(dayOfWeekTrxCondition)) {
            return "(%s)".formatted(
                    dayOfWeekTrxCondition.stream()
                            .map(this::dayConfig2DroolsCondition)
                            .collect(Collectors.joining(" || "))
            );
        } else {
            // no day configured
            return "false";
        }
    }

    private String dayConfig2DroolsCondition(DayOfWeekDTO.DayConfig dayConfig) {
        return "(trxChargeDate.dayOfWeek in (%s)%s)"
                .formatted(
                        dayConfig.getDaysOfWeek().stream().map(d -> DroolsTemplateRuleUtils.toTemplateParam(d).getParam()).collect(Collectors.joining(",")),
                        CollectionUtils.isEmpty(dayConfig.getIntervals())
                                ? "" // no interval configured
                                : " && (%s)"
                                .formatted(dayConfig.getIntervals().stream()
                                        .map(this::dayInterval2DroolsCondition)
                                        .collect(Collectors.joining(" || ")))
                );
    }

    private String dayInterval2DroolsCondition(DayOfWeekDTO.Interval interval) {
        return "(trxChargeDate.atZoneSameInstant(java.time.ZoneId.of(\"Europe/Rome\")).toLocalTime() >= %s && trxChargeDate.atZoneSameInstant(java.time.ZoneId.of(\"Europe/Rome\")).toLocalTime() <= %s)".formatted(
                DroolsTemplateRuleUtils.toTemplateParam(interval.getStartTime()).getParam()
                , DroolsTemplateRuleUtils.toTemplateParam(interval.getEndTime()).getParam()
        );
    }
}
