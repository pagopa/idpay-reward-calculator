package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

public abstract class BaseInitiativeTrxConsequence2DroolsRuleTransformer<T extends InitiativeTrxConsequence> implements InitiativeTrxConsequence2DroolsRuleTransformer<T> {

    private final TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade;

    protected BaseInitiativeTrxConsequence2DroolsRuleTransformer(TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade) {
        this.trxConsequence2DroolsRewardExpressionTransformerFacade = trxConsequence2DroolsRewardExpressionTransformerFacade;
    }

    protected int getTrxConsequenceRuleOrder(){
        return RewardConstants.INITIATIVE_TRX_CONSEQUENCE_ORDER;
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
                when $trx: %s()
                   eval($trx.getInitiativeRejectionReasons().get("%s") == null)
                then $trx.getRewards().put("%s", new %s(%s));
                end
                """.formatted(
                ruleName,
                getTrxConsequenceRuleName(),
                getTrxConsequenceRuleOrder(),
                initiativeId,
                TransactionDroolsDTO.class.getName(),
                initiativeId,
                initiativeId,
                Reward.class.getName(),
                trxConsequence2DroolsRewardExpressionTransformerFacade.apply(initiativeId, trxConsequence)
        );
    }
}
