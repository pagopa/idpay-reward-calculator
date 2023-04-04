package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.utils.RewardConstants;

public abstract class BaseInitiativeTrxConsequence2DroolsRuleTransformer<T extends InitiativeTrxConsequence> implements InitiativeTrxConsequence2DroolsRuleTransformer<T> {

    protected final TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade;

    protected BaseInitiativeTrxConsequence2DroolsRuleTransformer(TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade) {
        this.trxConsequence2DroolsRewardExpressionTransformerFacade = trxConsequence2DroolsRewardExpressionTransformerFacade;
    }

    protected int getTrxConsequenceRuleOrder(){
        return RewardConstants.INITIATIVE_TRX_CONSEQUENCE_ORDER;
    }

    protected abstract String getTrxConsequenceRuleName();

    @Override
    public String apply(String initiativeId, String organizationId, String ruleNamePrefix, T trxConsequence) {
        return initiativeTrxConsequenceRuleBuild(
                initiativeId,
                organizationId,
                ruleNamePrefix,
                trxConsequence
        );
    }

    protected String initiativeTrxConsequenceRuleBuild(String initiativeId, String organizationId, String ruleName, T trxConsequence) {
        return """
                                
                rule "%s-%s"
                salience %d
                agenda-group "%s"
                when
                   $userCounters: %s()
                   $userInitiativeCounters: %s() from $userCounters.initiatives.getOrDefault("%s", new %s("DUMMYUSERID", "%s"))
                   $trx: %s()
                   eval($trx.getInitiativeRejectionReasons().get("%s") == null)
                then %s
                end
                """.formatted(
                ruleName,
                getTrxConsequenceRuleName(),
                getTrxConsequenceRuleOrder(),
                initiativeId,
                UserInitiativeCountersWrapper.class.getName(),
                UserInitiativeCounters.class.getName(),
                initiativeId,
                UserInitiativeCounters.class.getName(),
                initiativeId,
                TransactionDroolsDTO.class.getName(),
                initiativeId,
                buildConsequences(
                        initiativeId,
                        organizationId,
                        trxConsequence
                )
        );
    }

    protected String buildConsequences(String initiativeId, String organizationId, T trxConsequence) {
        return "$trx.getRewards().put(\"%s\", new %s(\"%s\",\"%s\",%s));".formatted(
                initiativeId,
                Reward.class.getName(),
                initiativeId,
                organizationId,
                trxConsequence2DroolsRewardExpressionTransformerFacade.apply(initiativeId, trxConsequence)
        );
    }
}
