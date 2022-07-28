package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.trx.DayOfWeekDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

public class DayOfWeekTrxCondition2DroolsRuleTransformer extends BaseInitiativeTrxCondition2DroolsRuleTransformer<DayOfWeekDTO> implements InitiativeTrxCondition2DroolsRuleTransformer<DayOfWeekDTO> {

    public DayOfWeekTrxCondition2DroolsRuleTransformer(TrxCondition2DroolsConditionTransformerFacade trxCondition2DroolsConditionTransformerFacade) {
        super(trxCondition2DroolsConditionTransformerFacade);
    }

    @Override
    protected RewardConstants.InitiativeTrxConditionOrder getTrxConditionOrder() {
        return RewardConstants.InitiativeTrxConditionOrder.DAYOFWEEK;
    }
}
