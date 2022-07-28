package it.gov.pagopa.reward.drools.transformer.conditions;

import it.gov.pagopa.reward.drools.transformer.conditions.rules.*;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.trx.*;
import org.springframework.stereotype.Service;

@Service
public class TrxCondition2DroolsRuleTransformerFacadeImpl implements TrxCondition2DroolsRuleTransformerFacade {

    private final DayOfWeekTrxCondition2DroolsRuleTransformer dayOfWeekTrxConditionTransformer;
    private final MccFilterTrxCondition2DroolsRuleTransformer mccFilterTrxConditionTransformer;
    private final RewardLimitsTrxCondition2DroolsRuleTransformer rewardLimitsTrxConditionTransformer;
    private final ThresholdTrxCondition2DroolsRuleTransformer thresholdTrxConditionTransformer;
    private final TrxCountTrxCondition2DroolsRuleTransformer trxCountTrxConditionTransformer;
    private final RewardGroupsTrxCondition2DroolsRuleTransformer rewardGroupsTrxConditionTransformer;

    public TrxCondition2DroolsRuleTransformerFacadeImpl(TrxCondition2DroolsConditionTransformerFacade trxCondition2DroolsConditionTransformerFacade) {
        this.dayOfWeekTrxConditionTransformer = new DayOfWeekTrxCondition2DroolsRuleTransformer(trxCondition2DroolsConditionTransformerFacade);
        this.mccFilterTrxConditionTransformer = new MccFilterTrxCondition2DroolsRuleTransformer(trxCondition2DroolsConditionTransformerFacade);
        this.rewardLimitsTrxConditionTransformer = new RewardLimitsTrxCondition2DroolsRuleTransformer(trxCondition2DroolsConditionTransformerFacade);
        this.thresholdTrxConditionTransformer = new ThresholdTrxCondition2DroolsRuleTransformer(trxCondition2DroolsConditionTransformerFacade);
        this.trxCountTrxConditionTransformer = new TrxCountTrxCondition2DroolsRuleTransformer(trxCondition2DroolsConditionTransformerFacade);
        this.rewardGroupsTrxConditionTransformer = new RewardGroupsTrxCondition2DroolsRuleTransformer(trxCondition2DroolsConditionTransformerFacade);
    }

    @Override
    public String apply(String agendaGroup, String ruleNamePrefix, InitiativeTrxCondition trxCondition) {
        if(trxCondition == null){
            return "";
        }
        else if(trxCondition instanceof DayOfWeekDTO dayOfWeekTrxCondition){
            return dayOfWeekTrxConditionTransformer.apply(agendaGroup, ruleNamePrefix, dayOfWeekTrxCondition);
        }
        else if(trxCondition instanceof MccFilterDTO mccFilterTrxCondition){
            return mccFilterTrxConditionTransformer.apply(agendaGroup, ruleNamePrefix, mccFilterTrxCondition);
        }
        else if(trxCondition instanceof RewardLimitsDTO rewardLimitsTrxCondition){
            return rewardLimitsTrxConditionTransformer.apply(agendaGroup, ruleNamePrefix, rewardLimitsTrxCondition);
        }
        else if(trxCondition instanceof ThresholdDTO thresholdTrxCondition){
            return thresholdTrxConditionTransformer.apply(agendaGroup, ruleNamePrefix, thresholdTrxCondition);
        }
        else if(trxCondition instanceof TrxCountDTO trxCountTrxCondition){
            return trxCountTrxConditionTransformer.apply(agendaGroup, ruleNamePrefix, trxCountTrxCondition);
        }
        else if(trxCondition instanceof RewardGroupsDTO rewardGroupsTrxCondition){
            return rewardGroupsTrxConditionTransformer.apply(agendaGroup, ruleNamePrefix, rewardGroupsTrxCondition);
        }

        throw new IllegalStateException("InitiativeTrxCondition not handled: %s".formatted(trxCondition.getClass().getName()));
    }
}
