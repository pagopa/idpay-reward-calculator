package it.gov.pagopa.reward.drools.transformer.initiative_condition;

import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;

import java.util.stream.Collectors;

public class MccFilterRewardRule2DroolsConditionTransformer implements InitiativeRewardRule2DroolsConditionTransformer<MccFilterDTO> {
    @Override
    public String apply(MccFilterDTO mccFilterDTO) {
        return "mcc %s (%s)".formatted(
                mccFilterDTO.isAllowedList() ? "in" : "not in",
                mccFilterDTO.getValues().stream().map("\"%s\""::formatted).collect(Collectors.joining(","))
        );
    }
}
