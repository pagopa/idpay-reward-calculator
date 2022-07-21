package it.gov.pagopa.reward.service.build;

import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.model.DroolsRule;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class RewardRule2DroolsRuleImpl implements RewardRule2DroolsRule{
    //TODO
    @Override
    public Flux<DroolsRule> apply(Flux<InitiativeReward2BuildDTO> initiativeReward2BuildDTOFlux) {
        return initiativeReward2BuildDTOFlux.map(r->DroolsRule.builder().id(r.getInitiativeId()).build());
    }
}
