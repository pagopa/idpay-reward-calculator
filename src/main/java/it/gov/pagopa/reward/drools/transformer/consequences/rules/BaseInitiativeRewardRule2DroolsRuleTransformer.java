package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.RewardRule2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;
import it.gov.pagopa.reward.model.RewardTransaction;
import it.gov.pagopa.reward.utils.RewardConstants;

public abstract class BaseInitiativeRewardRule2DroolsRuleTransformer<T extends InitiativeRewardRule> implements InitiativeRewardRule2DroolsRuleTransformer<T> {

    private final RewardRule2DroolsRewardExpressionTransformerFacade rewardRule2DroolsRewardExpressionTransformerFacade;

    protected BaseInitiativeRewardRule2DroolsRuleTransformer(RewardRule2DroolsRewardExpressionTransformerFacade rewardRule2DroolsRewardExpressionTransformerFacade) {
        this.rewardRule2DroolsRewardExpressionTransformerFacade = rewardRule2DroolsRewardExpressionTransformerFacade;
    }

    protected int getRewardRuleOrder(){
        return RewardConstants.InitiativeTrxConditionOrder.values().length;
    }

    protected abstract String getRewardRuleName();

    @Override
    public String apply(String agendaGroup, String ruleNamePrefix, T rewardRule) {
        return initiativeRewardRuleBuild(
                agendaGroup,
                ruleNamePrefix,
                rewardRule
        );
    }

    protected String initiativeRewardRuleBuild(String initiativeId, String ruleName, T rewardRule) {
        return """
                                
                rule "%s-%s"
                salience %d
                agenda-group "%s"
                when
                   $trx: %s(rejectionReason.size() == 0)
                then $trx.getRewards().put("%s", "%s");
                end
                """.formatted(
                ruleName,
                getRewardRuleName(),
                getRewardRuleOrder(),
                initiativeId,
                RewardTransaction.class.getName(),
                initiativeId,
                rewardRule2DroolsRewardExpressionTransformerFacade.apply(rewardRule)
        );
    }
}
