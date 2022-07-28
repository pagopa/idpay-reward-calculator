package it.gov.pagopa.reward.drools.transformer.conditions;

import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;

public interface TrxCondition2DroolsRuleTransformerFacade {
    String apply(String agendaGroup, String ruleNamePrefix, InitiativeTrxCondition trxCondition);
}
