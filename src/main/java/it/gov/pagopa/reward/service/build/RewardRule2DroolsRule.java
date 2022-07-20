package it.gov.pagopa.reward.service.build;

import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.model.DroolsRule;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/** It will translate an initiative into a DroolsRule, returning null if invalid */ // TODO handle null return
public interface RewardRule2DroolsRule extends Function<Flux<InitiativeReward2BuildDTO>, Flux<DroolsRule>> {
}
