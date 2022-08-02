package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

public class RewardLimitsTrxCondition2DroolsRuleTransformer extends BaseInitiativeTrxCondition2DroolsRuleTransformer<RewardLimitsDTO> implements InitiativeTrxCondition2DroolsRuleTransformer<RewardLimitsDTO> {

    public RewardLimitsTrxCondition2DroolsRuleTransformer(TrxCondition2DroolsConditionTransformerFacade trxCondition2DroolsConditionTransformerFacade) {
        super(trxCondition2DroolsConditionTransformerFacade);
    }

    @Override
    protected RewardConstants.InitiativeTrxConditionOrder getTrxConditionOrder() {
        return RewardConstants.InitiativeTrxConditionOrder.REWARDLIMITS;
    }

    @Override
    public String apply(String agendaGroup, String ruleNamePrefix, RewardLimitsDTO rewardLimitsDTO) {
        return initiativeTrxConditionBuild(
                agendaGroup,
                "%s-%s".formatted(ruleNamePrefix, rewardLimitsDTO.getFrequency().name()),
                rewardLimitsDTO,
                RewardConstants.InitiativeTrxConditionOrder.REWARDLIMITS.getRejectionReason().formatted(rewardLimitsDTO.getFrequency().name())
        );
    }

    @Override
    protected String buildConditionNotMetConsequence(String initiativeId, String rejectionReason) {
        return """
                $trx.getRewards().remove("%s");
                   %s""".formatted(
                        initiativeId,
                        super.buildConditionNotMetConsequence(initiativeId, rejectionReason)
        );
    }
}
