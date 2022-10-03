package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.reward.drools.transformer.conditions.predicates.RewardLimitsTrxCondition2DroolsConditionTransformer;
import it.gov.pagopa.reward.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;

public class RewardLimitsTrxConsequence2DroolsExpressionTransformer implements InitiativeTrxConsequence2DroolsExpressionTransformer<RewardLimitsDTO> {
    @Override
    public String apply(String initiativeId, RewardLimitsDTO trxConsequence) {
        return "$trx.getRewards().get(\"%s\").getAccruedReward().min(java.math.BigDecimal.ZERO.max(%s.subtract(%s.getTotalReward().subtract($trx.getRefundInfo()!=null && $trx.getRefundInfo().getPreviousRewards()!=null && $trx.getRefundInfo().getPreviousRewards().get(\"%s\")!=null ? $trx.getRefundInfo().getPreviousRewards().get(\"%s\") : java.math.BigDecimal.ZERO)))).setScale(2, java.math.RoundingMode.HALF_DOWN)"
                .formatted(
                        initiativeId,
                        DroolsTemplateRuleUtils.toTemplateParam(trxConsequence.getRewardLimit()),
                        RewardLimitsTrxCondition2DroolsConditionTransformer.buildFrequencyCounterExpression(trxConsequence.getFrequency()),
                        initiativeId,
                        initiativeId
                );
    }
}
