package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;

public interface InitiativeTrxConsequence2DroolsRuleTransformer<T extends InitiativeTrxConsequence> {
    String apply(String initiativeId, String organizationId, String ruleNamePrefix, T trxConsequence);
}
