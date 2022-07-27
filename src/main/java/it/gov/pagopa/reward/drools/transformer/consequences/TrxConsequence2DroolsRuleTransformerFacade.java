package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;

public interface TrxConsequence2DroolsRuleTransformerFacade {
    String apply(String agendaGroup, String ruleNamePrefix, InitiativeTrxConsequence trxConsequence);
}
