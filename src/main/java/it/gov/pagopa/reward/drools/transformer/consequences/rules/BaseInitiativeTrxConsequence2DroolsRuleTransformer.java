package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
import it.gov.pagopa.reward.model.RewardTransaction;

public abstract class BaseInitiativeTrxConsequence2DroolsRuleTransformer<T extends InitiativeTrxConsequence> implements InitiativeTrxConsequence2DroolsRuleTransformer<T> {

    private final TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade;

    protected BaseInitiativeTrxConsequence2DroolsRuleTransformer(TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade) {
        this.trxConsequence2DroolsRewardExpressionTransformerFacade = trxConsequence2DroolsRewardExpressionTransformerFacade;
    }

    protected int getTrxConsequenceRuleOrder(){
        return -1;
    }

    protected abstract String getTrxConsequenceRuleName();

    @Override
    public String apply(String agendaGroup, String ruleNamePrefix, T trxConsequence) {
        return initiativeTrxConsequenceRuleBuild(
                agendaGroup,
                ruleNamePrefix,
                trxConsequence
        );
    }

    protected String initiativeTrxConsequenceRuleBuild(String initiativeId, String ruleName, T trxConsequence) {
        return """
                                
                rule "%s-%s"
                salience %d
                agenda-group "%s"
                when $trx: %s(rejectionReason.size() == 0)
                then $trx.getRewards().put("%s", %s);
                end
                """.formatted(
                ruleName,
                getTrxConsequenceRuleName(),
                getTrxConsequenceRuleOrder(),
                initiativeId,
                RewardTransaction.class.getName(),
                initiativeId,
                trxConsequence2DroolsRewardExpressionTransformerFacade.apply(trxConsequence)
        );
    }
}
