package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;

public interface InitiativeTrxCondition2DroolsConditionTransformer<T extends InitiativeTrxCondition> {
    String apply(String initiativeId, T initiativeTrxCondition);
}
