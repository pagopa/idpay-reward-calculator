package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

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
    public String apply(String initiativeId, String organizationId, String ruleNamePrefix, RewardLimitsDTO trxConsequence) {
        return initiativeTrxConsequenceRuleBuild(
                initiativeId,
                organizationId,
                "%s-%s".formatted(ruleNamePrefix, trxConsequence.getFrequency()),
                trxConsequence
        );
    }

    @Override
    protected String buildConsequences(String initiativeId, String organizationId, RewardLimitsDTO trxConsequence) {
        return """
                %s
                   %s reward = $trx.getRewards().get("%s");
                   if(reward != null){
                      java.lang.Long oldAccruedRewardCents=reward.getAccruedRewardCents();
                      reward.setAccruedRewardCents(%s);
                      if(reward.getAccruedRewardCents().compareTo(oldAccruedRewardCents) != 0){
                         reward.set%sCapped(true);
                         %s
                      }
                   }""".formatted(
                "",
                Reward.class.getName(),
                initiativeId,
                trxConsequence2DroolsRewardExpressionTransformerFacade.apply(initiativeId, trxConsequence),
                StringUtils.capitalize(trxConsequence.getFrequency().name().toLowerCase()),
                Arrays.stream(RewardLimitsDTO.RewardLimitFrequency.values())
                        .filter(f->!f.equals(trxConsequence.getFrequency()))
                        .map(f->"reward.set%sCapped(false);".formatted(StringUtils.capitalize(f.name().toLowerCase())))
                        .collect(Collectors.joining("\n         "))
        );
    }
}
