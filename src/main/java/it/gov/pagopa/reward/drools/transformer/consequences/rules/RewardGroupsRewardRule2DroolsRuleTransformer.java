package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.RewardRule2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;

public class RewardGroupsRewardRule2DroolsRuleTransformer extends BaseInitiativeRewardRule2DroolsRuleTransformer<RewardGroupsDTO> implements InitiativeRewardRule2DroolsRuleTransformer<RewardGroupsDTO> {

    public RewardGroupsRewardRule2DroolsRuleTransformer(RewardRule2DroolsRewardExpressionTransformerFacade rewardRule2DroolsRewardExpressionTransformerFacade) {
        super(rewardRule2DroolsRewardExpressionTransformerFacade);
    }

    @Override
    protected String getRewardRuleName() {
        return "REWARDGROUPS";
    }

}
