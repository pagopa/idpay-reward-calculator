package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;

public class RewardLimitsTrxConsequence2DroolsExpressionTransformer implements InitiativeTrxConsequence2DroolsExpressionTransformer<RewardLimitsDTO> {
    @Override
    public String apply(RewardLimitsDTO trxConsequence) {
        return "java.math.BigDecimal.ZERO"; //TODO
    }
}
