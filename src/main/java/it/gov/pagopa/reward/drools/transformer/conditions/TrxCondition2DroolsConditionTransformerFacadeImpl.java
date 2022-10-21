package it.gov.pagopa.reward.drools.transformer.conditions;

import it.gov.pagopa.reward.drools.transformer.conditions.predicates.*;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.trx.*;
import org.springframework.stereotype.Service;

@Service
public class TrxCondition2DroolsConditionTransformerFacadeImpl implements TrxCondition2DroolsConditionTransformerFacade {

    private final DayOfWeekTrxCondition2DroolsConditionTransformer dayOfWeekTrxConditionTransformer;
    private final MccFilterTrxCondition2DroolsConditionTransformer mccFilterTrxConditionTransformer;
    private final RewardLimitsTrxCondition2DroolsConditionTransformer rewardLimitsTrxConditionTransformer;
    private final ThresholdTrxCondition2DroolsConditionTransformer thresholdTrxConditionTransformer;
    private final TrxCountTrxCondition2DroolsConditionTransformer trxCountTrxConditionTransformer;
    private final RewardGroupsTrxCondition2DroolsConditionTransformer rewardGroupsTrxConditionTransformer;

    public TrxCondition2DroolsConditionTransformerFacadeImpl() {
        this.dayOfWeekTrxConditionTransformer = new DayOfWeekTrxCondition2DroolsConditionTransformer();
        this.mccFilterTrxConditionTransformer = new MccFilterTrxCondition2DroolsConditionTransformer();
        this.rewardLimitsTrxConditionTransformer = new RewardLimitsTrxCondition2DroolsConditionTransformer();
        this.thresholdTrxConditionTransformer = new ThresholdTrxCondition2DroolsConditionTransformer();
        this.trxCountTrxConditionTransformer = new TrxCountTrxCondition2DroolsConditionTransformer();
        this.rewardGroupsTrxConditionTransformer = new RewardGroupsTrxCondition2DroolsConditionTransformer();
    }

    @Override
    public String apply(String initiativeId, InitiativeTrxCondition trxCondition) {
        if(trxCondition instanceof DayOfWeekDTO dayOfWeekTrxCondition){
            return dayOfWeekTrxConditionTransformer.apply(initiativeId, dayOfWeekTrxCondition);
        }
        else if(trxCondition instanceof MccFilterDTO mccFilterTrxCondition){
            return mccFilterTrxConditionTransformer.apply(initiativeId, mccFilterTrxCondition);
        }
        else if(trxCondition instanceof RewardLimitsDTO rewardLimitsTrxCondition){
            return rewardLimitsTrxConditionTransformer.apply(initiativeId, rewardLimitsTrxCondition);
        }
        else if(trxCondition instanceof ThresholdDTO thresholdTrxCondition){
            return thresholdTrxConditionTransformer.apply(initiativeId, thresholdTrxCondition);
        }
        else if(trxCondition instanceof TrxCountDTO trxCountTrxCondition){
            return trxCountTrxConditionTransformer.apply(initiativeId, trxCountTrxCondition);
        }
        else if(trxCondition instanceof RewardGroupsDTO rewardGroupsTrxCondition){
            return rewardGroupsTrxConditionTransformer.apply(initiativeId, rewardGroupsTrxCondition);
        }

        throw new IllegalStateException("InitiativeTrxCondition not handled: %s".formatted(trxCondition.getClass().getName()));
    }
}
