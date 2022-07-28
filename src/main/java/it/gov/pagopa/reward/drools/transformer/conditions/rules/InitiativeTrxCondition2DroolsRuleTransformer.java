package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;

public interface InitiativeTrxCondition2DroolsRuleTransformer<T extends InitiativeTrxCondition> {
    String apply(String agendaGroup, String ruleNamePrefix, T trxCondition);
}
