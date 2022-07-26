package it.gov.pagopa.reward.drools.transformer;

import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;

public interface InitiativeRewardRule2DroolsConditionTransformerFacade {
    String apply(InitiativeTrxCondition initiativeRewardRule);
}
