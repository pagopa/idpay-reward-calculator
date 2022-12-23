package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;

public class TrxCountTrxConsequence2DroolsExpressionTransformer implements InitiativeTrxConsequence2DroolsExpressionTransformer<TrxCountDTO> {

    @Override
    public String apply(String initiativeId, TrxCountDTO trxConsequence) {
        return "java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.UNNECESSARY)";
    }

}
