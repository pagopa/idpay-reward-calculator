package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardGroupsRewardRule2DroolsRuleTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardLimitsRewardRule2DroolsRuleTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardValueRewardRule2DroolsRuleTransformer;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import org.springframework.stereotype.Service;

@Service
public class RewardRule2DroolsRuleTransformerFacadeImpl implements RewardRule2DroolsRuleTransformerFacade {

    private final RewardValueRewardRule2DroolsRuleTransformer rewardValueRewardRuleTransformer;
    private final RewardGroupsRewardRule2DroolsRuleTransformer rewardGroupsRewardRuleTransformer;
    private final RewardLimitsRewardRule2DroolsRuleTransformer rewardLimitsRewardRuleTransformer;

    public RewardRule2DroolsRuleTransformerFacadeImpl(RewardRule2DroolsRewardExpressionTransformerFacade rewardRule2DroolsRewardExpressionTransformerFacade) {
        this.rewardValueRewardRuleTransformer = new RewardValueRewardRule2DroolsRuleTransformer(rewardRule2DroolsRewardExpressionTransformerFacade);
        this.rewardGroupsRewardRuleTransformer = new RewardGroupsRewardRule2DroolsRuleTransformer(rewardRule2DroolsRewardExpressionTransformerFacade);
        this.rewardLimitsRewardRuleTransformer = new RewardLimitsRewardRule2DroolsRuleTransformer(rewardRule2DroolsRewardExpressionTransformerFacade);
    }

    @Override
    public String apply(String agendaGroup, String ruleNamePrefix, InitiativeRewardRule initiativeRewardRule) {
        if(initiativeRewardRule == null){
            return "";
        }
        else if(initiativeRewardRule instanceof RewardValueDTO rewardValueDTO){
            return rewardValueRewardRuleTransformer.apply(agendaGroup, ruleNamePrefix, rewardValueDTO);
        }
        else if(initiativeRewardRule instanceof RewardGroupsDTO rewardGroupsDTO){
            return rewardGroupsRewardRuleTransformer.apply(agendaGroup, ruleNamePrefix, rewardGroupsDTO);
        }
        else if(initiativeRewardRule instanceof RewardLimitsDTO rewardLimitsDTO){
            return rewardLimitsRewardRuleTransformer.apply(agendaGroup, ruleNamePrefix, rewardLimitsDTO);
        }

        throw new IllegalStateException("InitiativeRewardRule not handled: %s".formatted(initiativeRewardRule.getClass().getName()));
    }
}
