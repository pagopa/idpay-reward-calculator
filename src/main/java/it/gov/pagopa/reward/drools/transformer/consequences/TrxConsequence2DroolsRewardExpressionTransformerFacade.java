package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;

public interface TrxConsequence2DroolsRewardExpressionTransformerFacade {
    String apply(InitiativeTrxConsequence trxConsequence);
}
