package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardGroupsRewardRule2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardLimitsRewardRule2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardValueRewardRule2DroolsExpressionTransformer;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import org.springframework.stereotype.Service;

@Service
public class RewardRule2DroolsRewardExpressionTransformerFacadeImpl implements RewardRule2DroolsRewardExpressionTransformerFacade {

    private final RewardValueRewardRule2DroolsExpressionTransformer rewardValueRewardRuleTransformer;
    private final RewardGroupsRewardRule2DroolsExpressionTransformer rewardGroupsRewardRuleTransformer;
    private final RewardLimitsRewardRule2DroolsExpressionTransformer rewardLimitsRewardRuleTransformer;

    public RewardRule2DroolsRewardExpressionTransformerFacadeImpl() {
        this.rewardValueRewardRuleTransformer = new RewardValueRewardRule2DroolsExpressionTransformer();
        this.rewardGroupsRewardRuleTransformer = new RewardGroupsRewardRule2DroolsExpressionTransformer();
        this.rewardLimitsRewardRuleTransformer = new RewardLimitsRewardRule2DroolsExpressionTransformer();
    }

    @Override
    public String apply(InitiativeRewardRule initiativeRewardRule) {
        if(initiativeRewardRule == null){
            return "";
        }
        else if(initiativeRewardRule instanceof RewardValueDTO rewardValueDTO){
            return rewardValueRewardRuleTransformer.apply(rewardValueDTO);
        }
        else if(initiativeRewardRule instanceof RewardGroupsDTO rewardGroupsDTO){
            return rewardGroupsRewardRuleTransformer.apply(rewardGroupsDTO);
        }
        else if(initiativeRewardRule instanceof RewardLimitsDTO rewardLimitsDTO){
            return rewardLimitsRewardRuleTransformer.apply(rewardLimitsDTO);
        }

        throw new IllegalStateException("InitiativeRewardRule not handled: %s".formatted(initiativeRewardRule.getClass().getName()));
    }
}
