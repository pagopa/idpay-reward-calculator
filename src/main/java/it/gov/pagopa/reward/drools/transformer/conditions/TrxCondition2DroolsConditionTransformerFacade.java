package it.gov.pagopa.reward.drools.transformer.conditions;

import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;

public interface TrxCondition2DroolsConditionTransformerFacade {
    String apply(String initiativeId, InitiativeTrxCondition trxCondition);
}
