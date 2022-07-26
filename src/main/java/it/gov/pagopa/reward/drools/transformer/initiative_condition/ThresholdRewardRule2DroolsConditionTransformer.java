package it.gov.pagopa.reward.drools.transformer.initiative_condition;

import it.gov.pagopa.reward.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import org.springframework.data.util.Pair;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThresholdRewardRule2DroolsConditionTransformer implements InitiativeRewardRule2DroolsConditionTransformer<ThresholdDTO> {
    @Override
    public String apply(ThresholdDTO thresholdDTO) {
        return Stream.of(
                        checkDefinedThreshold(thresholdDTO.getFrom(), thresholdDTO.isFromIncluded(), ">"),
                        checkDefinedThreshold(thresholdDTO.getTo(), thresholdDTO.isToIncluded(), "<")
                ).filter(Objects::nonNull)
                .map(op ->
                        "amount %s %s".formatted(
                                op.getFirst(),
                                DroolsTemplateRuleUtils.toTemplateParam(op.getSecond())
                        )
                ).collect(Collectors.joining(" and "));
    }

    private Pair<String, BigDecimal> checkDefinedThreshold(BigDecimal threshold, boolean inclusive, String operator) {
        return threshold != null
                ? Pair.of(inclusive ? "%s=".formatted(operator) : operator, threshold)
                : null;
    }
}
