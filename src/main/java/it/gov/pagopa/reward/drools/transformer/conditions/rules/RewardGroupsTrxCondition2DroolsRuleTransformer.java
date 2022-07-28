package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

public class RewardGroupsTrxCondition2DroolsRuleTransformer extends BaseInitiativeTrxCondition2DroolsRuleTransformer<RewardGroupsDTO> implements InitiativeTrxCondition2DroolsRuleTransformer<RewardGroupsDTO> {

    public RewardGroupsTrxCondition2DroolsRuleTransformer(TrxCondition2DroolsConditionTransformerFacade trxCondition2DroolsConditionTransformerFacade) {
        super(trxCondition2DroolsConditionTransformerFacade);
    }

    @Override
    protected RewardConstants.InitiativeTrxConditionOrder getTrxConditionOrder() {
        return RewardConstants.InitiativeTrxConditionOrder.REWARDGROUP;
    }

}
