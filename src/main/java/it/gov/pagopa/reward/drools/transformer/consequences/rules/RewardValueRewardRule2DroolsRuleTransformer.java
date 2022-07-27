package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.RewardRule2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.drools.transformer.consequences.RewardRule2DroolsRuleTransformerFacade;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

public class RewardValueRewardRule2DroolsRuleTransformer extends BaseInitiativeRewardRule2DroolsRuleTransformer<RewardValueDTO> implements InitiativeRewardRule2DroolsRuleTransformer<RewardValueDTO> {

    public RewardValueRewardRule2DroolsRuleTransformer(RewardRule2DroolsRewardExpressionTransformerFacade rewardRule2DroolsRewardExpressionTransformerFacade) {
        super(rewardRule2DroolsRewardExpressionTransformerFacade);
    }

    @Override
    protected String getRewardRuleName() {
        return "REWARDVALUE";
    }

}
