package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;

public interface InitiativeRewardRule2DroolsExpressionTransformer<T extends InitiativeRewardRule> {
    String apply(T rewardRule);
}
