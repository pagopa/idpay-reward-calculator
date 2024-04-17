package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.reward.drools.transformer.conditions.predicates.RewardLimitsTrxCondition2DroolsConditionTransformer;
import it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;

public class RewardLimitsTrxConsequence2DroolsExpressionTransformer implements InitiativeTrxConsequence2DroolsExpressionTransformer<RewardLimitsDTO> {
    @Override
    public String apply(String initiativeId, RewardLimitsDTO trxConsequence) {
        return "java.lang.Long.min($trx.getRewards().get(\"%s\").getAccruedRewardCents(), java.lang.Long.max(0L, %s - (%s.getTotalRewardCents() - ($trx.getRefundInfo()!=null && $trx.getRefundInfo().getPreviousRewards()!=null && $trx.getRefundInfo().getPreviousRewards().get(\"%s\")!=null ? $trx.getRefundInfo().getPreviousRewards().get(\"%s\").getAccruedRewardCents() : 0L ))))"
                .formatted(
                        initiativeId,
                        DroolsTemplateRuleUtils.toTemplateParam(trxConsequence.getRewardLimitCents()),
                        RewardLimitsTrxCondition2DroolsConditionTransformer.buildFrequencyCounterExpression(trxConsequence.getFrequency()),
                        initiativeId,
                        initiativeId
                );
    }
}
