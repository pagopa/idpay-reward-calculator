package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.RewardRule2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;

public class RewardLimitsRewardRule2DroolsRuleTransformer extends BaseInitiativeRewardRule2DroolsRuleTransformer<RewardLimitsDTO> implements InitiativeRewardRule2DroolsRuleTransformer<RewardLimitsDTO> {

    public RewardLimitsRewardRule2DroolsRuleTransformer(RewardRule2DroolsRewardExpressionTransformerFacade rewardRule2DroolsRewardExpressionTransformerFacade) {
        super(rewardRule2DroolsRewardExpressionTransformerFacade);
    }

    @Override
    protected String getRewardRuleName() {
        return "";
    }

    @Override
    public String apply(String agendaGroup, String ruleNamePrefix, RewardLimitsDTO rewardRule) {
        return initiativeRewardRuleBuild(
                agendaGroup,
                "%s-REWARDLIMITS-%s".formatted(ruleNamePrefix, rewardRule.getFrequency()),
                rewardRule
        );
    }
}
