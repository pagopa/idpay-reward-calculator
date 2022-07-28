package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;

public class RewardLimitsTrxCondition2DroolsConditionTransformer implements InitiativeTrxCondition2DroolsConditionTransformer<RewardLimitsDTO> {
    @Override
    public String apply(RewardLimitsDTO rewardLimitsDTO) {
        return "true";//TODO
    }
}
