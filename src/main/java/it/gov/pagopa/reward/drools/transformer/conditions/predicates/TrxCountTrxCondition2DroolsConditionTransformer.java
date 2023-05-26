package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;
import org.springframework.data.util.Pair;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TrxCountTrxCondition2DroolsConditionTransformer implements InitiativeTrxCondition2DroolsConditionTransformer<TrxCountDTO> {
    @Override
    public String apply(String initiativeId, TrxCountDTO trxCountDTO) {
        return Stream.of(
                        checkDefinedThreshold(trxCountDTO.getFrom(), trxCountDTO.isFromIncluded(), ">"),
                        checkDefinedThreshold(trxCountDTO.getTo(), trxCountDTO.isToIncluded(), "<")
                ).filter(Objects::nonNull)
                .map(op ->
                        "$userInitiativeCounters.trxNumber %s %s".formatted(
                                op.getFirst(),
                                DroolsTemplateRuleUtils.toTemplateParam(op.getSecond() - 1L) // -1 in order to count the current transaction
                        )
                ).collect(Collectors.joining(" && "));
    }

    private Pair<String, Long> checkDefinedThreshold(Long threshold, boolean inclusive, String operator) {
        if(threshold==null){
            return null;
        } else{
            return Pair.of(inclusive ? "%s=".formatted(operator) : operator, threshold);
        }
    }
}
