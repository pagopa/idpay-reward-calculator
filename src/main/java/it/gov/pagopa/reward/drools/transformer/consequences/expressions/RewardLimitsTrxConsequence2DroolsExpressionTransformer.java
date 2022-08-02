package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.reward.drools.transformer.conditions.predicates.RewardLimitsTrxCondition2DroolsConditionTransformer;
import it.gov.pagopa.reward.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;

public class RewardLimitsTrxConsequence2DroolsExpressionTransformer implements InitiativeTrxConsequence2DroolsExpressionTransformer<RewardLimitsDTO> {
    @Override
    public String apply(String initiativeId, RewardLimitsDTO trxConsequence) {
        return "java.math.BigDecimal.min($trx.rewards.get(\"%s\").accruedReward, %s.subtract(%s.totalReward))"
                .formatted(
                        initiativeId,
                        DroolsTemplateRuleUtils.toTemplateParam(trxConsequence.getRewardLimit()),
                        RewardLimitsTrxCondition2DroolsConditionTransformer.buildFrequencyCounterExpression(trxConsequence.getFrequency())
                );
    }
}
