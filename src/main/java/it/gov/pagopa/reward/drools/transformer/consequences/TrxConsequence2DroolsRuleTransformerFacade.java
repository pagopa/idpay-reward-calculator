package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;

public interface TrxConsequence2DroolsRuleTransformerFacade {
    String apply(String initiativeId, String organizationId, String ruleNamePrefix, InitiativeTrxConsequence trxConsequence);
}
