package it.gov.pagopa.reward.service.build;

import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.model.DroolsRule;

import java.util.function.Function;

/** It will translate an initiative into a DroolsRule, returning null if invalid */
public interface RewardRule2DroolsRuleService extends Function<InitiativeReward2BuildDTO, DroolsRule> {
}
