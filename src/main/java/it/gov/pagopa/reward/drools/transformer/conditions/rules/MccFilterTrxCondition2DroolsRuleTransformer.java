package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

public class MccFilterTrxCondition2DroolsRuleTransformer extends BaseInitiativeTrxCondition2DroolsRuleTransformer<MccFilterDTO> implements InitiativeTrxCondition2DroolsRuleTransformer<MccFilterDTO> {

    public MccFilterTrxCondition2DroolsRuleTransformer(TrxCondition2DroolsConditionTransformerFacade trxCondition2DroolsConditionTransformerFacade) {
        super(trxCondition2DroolsConditionTransformerFacade);
    }

    @Override
    protected RewardConstants.InitiativeTrxConditionOrder getTrxConditionOrder() {
        return RewardConstants.InitiativeTrxConditionOrder.MCCFILTER;
    }

}
