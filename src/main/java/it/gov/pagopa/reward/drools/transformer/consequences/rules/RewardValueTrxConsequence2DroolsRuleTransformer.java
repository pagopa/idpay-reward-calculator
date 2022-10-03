package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;

public class RewardValueTrxConsequence2DroolsRuleTransformer extends BaseInitiativeTrxConsequence2DroolsRuleTransformer<RewardValueDTO> implements InitiativeTrxConsequence2DroolsRuleTransformer<RewardValueDTO> {

    public RewardValueTrxConsequence2DroolsRuleTransformer(TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade) {
        super(trxConsequence2DroolsRewardExpressionTransformerFacade);
    }

    @Override
    protected String getTrxConsequenceRuleName() {
        return "REWARDVALUE";
    }

}
