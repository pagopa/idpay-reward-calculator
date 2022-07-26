package it.gov.pagopa.reward.drools.transformer;

import it.gov.pagopa.reward.drools.transformer.initiative_condition.*;
import it.gov.pagopa.reward.dto.rule.trx.*;
import org.springframework.stereotype.Service;

@Service
public class InitiativeRewardRule2DroolsConditionTransformerFacadeImpl implements InitiativeRewardRule2DroolsConditionTransformerFacade {

    private final DayOfWeekRewardRule2DroolsConditionTransformer dayOfWeekRewardRuleTransformer;
    private final MccFilterRewardRule2DroolsConditionTransformer mccFilterRewardRuleTransformer;
    private final RewardLimitsRewardRule2DroolsConditionTransformer rewardLimitsRewardRuleTransformer;
    private final ThresholdRewardRule2DroolsConditionTransformer thresholdRewardRuleTransformer;
    private final TrxCountRewardRule2DroolsConditionTransformer trxCountRewardRuleTransformer;

    public InitiativeRewardRule2DroolsConditionTransformerFacadeImpl() {
        this.dayOfWeekRewardRuleTransformer = new DayOfWeekRewardRule2DroolsConditionTransformer();
        this.mccFilterRewardRuleTransformer = new MccFilterRewardRule2DroolsConditionTransformer();
        this.rewardLimitsRewardRuleTransformer = new RewardLimitsRewardRule2DroolsConditionTransformer();
        this.thresholdRewardRuleTransformer = new ThresholdRewardRule2DroolsConditionTransformer();
        this.trxCountRewardRuleTransformer = new TrxCountRewardRule2DroolsConditionTransformer();
    }

    @Override
    public String apply(InitiativeTrxCondition initiativeRewardRule) {
        if(initiativeRewardRule instanceof DayOfWeekDTO dayOfWeekRewardRule){
            return dayOfWeekRewardRuleTransformer.apply(dayOfWeekRewardRule);
        }
        else if(initiativeRewardRule instanceof MccFilterDTO mccFilterRewardRule){
            return mccFilterRewardRuleTransformer.apply(mccFilterRewardRule);
        }
        else if(initiativeRewardRule instanceof RewardLimitsDTO rewardLimitsRewardRule){
            return rewardLimitsRewardRuleTransformer.apply(rewardLimitsRewardRule);
        }
        else if(initiativeRewardRule instanceof ThresholdDTO thresholdRewardRule){
            return thresholdRewardRuleTransformer.apply(thresholdRewardRule);
        }
        else if(initiativeRewardRule instanceof TrxCountDTO trxCountRewardRule){
            return trxCountRewardRuleTransformer.apply(trxCountRewardRule);
        }

        throw new IllegalStateException("InitiativeRewardRule not handled: %s".formatted(initiativeRewardRule.getClass().getName()));
    }
}
