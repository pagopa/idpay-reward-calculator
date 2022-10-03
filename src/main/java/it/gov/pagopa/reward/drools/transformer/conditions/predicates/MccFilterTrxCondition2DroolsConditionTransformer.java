package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;

import java.util.stream.Collectors;

public class MccFilterTrxCondition2DroolsConditionTransformer implements InitiativeTrxCondition2DroolsConditionTransformer<MccFilterDTO> {
    @Override
    public String apply(String initiativeId, MccFilterDTO mccFilterDTO) {
        return "mcc %s (%s)".formatted(
                mccFilterDTO.isAllowedList() ? "in" : "not in",
                mccFilterDTO.getValues().stream().map("\"%s\""::formatted).collect(Collectors.joining(","))
        );
    }
}
