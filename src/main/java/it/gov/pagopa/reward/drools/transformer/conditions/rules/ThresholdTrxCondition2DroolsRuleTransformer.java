package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

public class ThresholdTrxCondition2DroolsRuleTransformer extends BaseInitiativeTrxCondition2DroolsRuleTransformer<ThresholdDTO> implements InitiativeTrxCondition2DroolsRuleTransformer<ThresholdDTO> {

    public ThresholdTrxCondition2DroolsRuleTransformer(TrxCondition2DroolsConditionTransformerFacade trxCondition2DroolsConditionTransformerFacade) {
        super(trxCondition2DroolsConditionTransformerFacade);
    }

    @Override
    protected RewardConstants.InitiativeTrxConditionOrder getTrxConditionOrder() {
        return RewardConstants.InitiativeTrxConditionOrder.THRESHOLD;
    }

}
