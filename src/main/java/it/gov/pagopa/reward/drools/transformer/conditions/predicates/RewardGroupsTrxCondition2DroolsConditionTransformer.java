package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;

import java.util.stream.Collectors;

public class RewardGroupsTrxCondition2DroolsConditionTransformer implements InitiativeTrxCondition2DroolsConditionTransformer<RewardGroupsDTO> {
    @Override
    public String apply(String initiativeId, RewardGroupsDTO rewardGroups) {
        return "(%s)".formatted(rewardGroups.getRewardGroups().stream()
                .map(rg ->
                        "(amount >= %s && amount <= %s)".formatted(
                                DroolsTemplateRuleUtils.toTemplateParam(rg.getFrom()).getParam(),
                                DroolsTemplateRuleUtils.toTemplateParam(rg.getTo()).getParam()
                        )
                ).collect(Collectors.joining(" || ")));
    }
}
