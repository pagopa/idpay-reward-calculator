package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import org.springframework.data.util.Pair;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThresholdTrxCondition2DroolsConditionTransformer implements InitiativeTrxCondition2DroolsConditionTransformer<ThresholdDTO> {
    @Override
    public String apply(String initiativeId, ThresholdDTO thresholdDTO) {
        return Stream.of(
                        checkDefinedThreshold(thresholdDTO.getFromCents(), thresholdDTO.isFromIncluded(), ">"),
                        checkDefinedThreshold(thresholdDTO.getToCents(), thresholdDTO.isToIncluded(), "<")
                ).filter(Objects::nonNull)
                .map(op ->
                        "effectiveAmountCents %s %s".formatted(
                                op.getFirst(),
                                DroolsTemplateRuleUtils.toTemplateParam(op.getSecond())
                        )
                ).collect(Collectors.joining(" && "));
    }

    private Pair<String, Long> checkDefinedThreshold(Long thresholdCents, boolean inclusive, String operator) {
        if(thresholdCents==null){
            return null;
        } else{
            return Pair.of(inclusive ? "%s=".formatted(operator) : operator, thresholdCents);
        }
    }
}
