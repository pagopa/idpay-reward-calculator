package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.commons.lang3.StringUtils;

public class RewardLimitsTrxConsequence2DroolsRuleTransformer extends BaseInitiativeTrxConsequence2DroolsRuleTransformer<RewardLimitsDTO> implements InitiativeTrxConsequence2DroolsRuleTransformer<RewardLimitsDTO> {

    public RewardLimitsTrxConsequence2DroolsRuleTransformer(TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade) {
        super(trxConsequence2DroolsRewardExpressionTransformerFacade);
    }

    @Override
    protected String getTrxConsequenceRuleName() {
        return "REWARDLIMITS-CAP";
    }

    @Override
    protected int getTrxConsequenceRuleOrder() {
        return RewardConstants.INITIATIVE_TRX_CONSEQUENCE_REWARD_LIMITS_ORDER;
    }

    @Override
    public String apply(String agendaGroup, String ruleNamePrefix, RewardLimitsDTO trxConsequence) {
        return initiativeTrxConsequenceRuleBuild(
                agendaGroup,
                "%s-%s".formatted(ruleNamePrefix, trxConsequence.getFrequency()),
                trxConsequence
        );
    }

    @Override
    protected String buildConsequences(String initiativeId, RewardLimitsDTO trxConsequence) {
        return """
                %s
                   %s reward = $trx.getRewards().get("%s");
                   if(reward != null){
                      java.math.BigDecimal oldAccruedReward=reward.getAccruedReward();
                      reward.setAccruedReward(%s);
                      if(reward.getAccruedReward().compareTo(oldAccruedReward) != 0){
                         reward.set%sCapped(true);
                      }
                   }""".formatted(
                "",
                Reward.class.getName(),
                initiativeId,
                trxConsequence2DroolsRewardExpressionTransformerFacade.apply(initiativeId, trxConsequence),
                StringUtils.capitalize(trxConsequence.getFrequency().name().toLowerCase())
        );
    }
}
