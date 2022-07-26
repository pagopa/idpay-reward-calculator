package it.gov.pagopa.reward.drools;

import it.gov.pagopa.reward.drools.transformer.DayOfWeekRewardRule2DroolsConditionTransformer;
import it.gov.pagopa.reward.dto.rule.trx.DayOfWeekDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;
import org.springframework.stereotype.Service;

@Service
public class InitiativeRewardRule2DroolsConditionTransformerFacadeImpl implements InitiativeRewardRule2DroolsConditionTransformerFacade {

    private final DayOfWeekRewardRule2DroolsConditionTransformer dayOfWeekRewardRuleTransformer;

    public InitiativeRewardRule2DroolsConditionTransformerFacadeImpl() {
        this.dayOfWeekRewardRuleTransformer = new DayOfWeekRewardRule2DroolsConditionTransformer();
    }

    @Override
    public String apply(InitiativeTrxCondition initiativeRewardRule) {
        if(initiativeRewardRule instanceof DayOfWeekDTO dayOfWeekRewardRule){
            return dayOfWeekRewardRuleTransformer.apply(dayOfWeekRewardRule);
        }

        throw new IllegalStateException("InitiativeRewardRule not handled: %s".formatted(initiativeRewardRule.getClass().getName()));
    }
}
