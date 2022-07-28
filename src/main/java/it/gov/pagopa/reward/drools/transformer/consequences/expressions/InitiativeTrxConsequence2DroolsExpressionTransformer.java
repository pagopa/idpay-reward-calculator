package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;

public interface InitiativeTrxConsequence2DroolsExpressionTransformer<T extends InitiativeTrxConsequence> {
    String apply(T trxConsequence);
}
