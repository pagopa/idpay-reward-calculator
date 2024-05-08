package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;

import java.util.stream.Collectors;

public class RewardGroupsTrxCondition2DroolsConditionTransformer implements InitiativeTrxCondition2DroolsConditionTransformer<RewardGroupsDTO> {
    @Override
    public String apply(String initiativeId, RewardGroupsDTO rewardGroups) {
        return "(%s)".formatted(rewardGroups.getRewardGroups().stream()
                .map(rg ->
                        "(effectiveAmountCents >= %s && effectiveAmountCents <= %s)".formatted(
                                DroolsTemplateRuleUtils.toTemplateParam(rg.getFromCents()).getParam(),
                                DroolsTemplateRuleUtils.toTemplateParam(rg.getToCents()).getParam()
                        )
                ).collect(Collectors.joining(" || ")));
    }
}
