package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;

public interface RewardRule2DroolsRuleTransformerFacade {
    String apply(String agendaGroup, String ruleNamePrefix, InitiativeRewardRule rewardRule);
}
