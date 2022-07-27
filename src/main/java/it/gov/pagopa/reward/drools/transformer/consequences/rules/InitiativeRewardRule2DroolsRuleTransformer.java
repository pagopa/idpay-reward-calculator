package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;

public interface InitiativeRewardRule2DroolsRuleTransformer<T extends InitiativeRewardRule> {
    String apply(String agendaGroup, String ruleNamePrefix, T rewardRule);
}
