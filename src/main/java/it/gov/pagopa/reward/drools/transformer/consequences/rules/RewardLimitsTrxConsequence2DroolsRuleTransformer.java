package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

public class RewardLimitsTrxConsequence2DroolsRuleTransformer extends BaseInitiativeTrxConsequence2DroolsRuleTransformer<RewardLimitsDTO> implements InitiativeTrxConsequence2DroolsRuleTransformer<RewardLimitsDTO> {

    public RewardLimitsTrxConsequence2DroolsRuleTransformer(TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade) {
        super(trxConsequence2DroolsRewardExpressionTransformerFacade);
    }

    @Override
    protected String getTrxConsequenceRuleName() {
        return "";
    }

    @Override
    protected int getTrxConsequenceRuleOrder() {
        return RewardConstants.INITIATIVE_TRX_CONSEQUENCE_REWARD_LIMITS_ORDER;
    }

    @Override
    public String apply(String agendaGroup, String ruleNamePrefix, RewardLimitsDTO trxConsequence) {
        return initiativeTrxConsequenceRuleBuild(
                agendaGroup,
                "%s-REWARDLIMITS-%s".formatted(ruleNamePrefix, trxConsequence.getFrequency()),
                trxConsequence
        );
    }
}
