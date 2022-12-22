package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

public class TrxCountTrxConsequence2DroolsRuleTransformer extends BaseInitiativeTrxConsequence2DroolsRuleTransformer<TrxCountDTO> implements InitiativeTrxConsequence2DroolsRuleTransformer<TrxCountDTO> {

    public TrxCountTrxConsequence2DroolsRuleTransformer(TrxConsequence2DroolsRewardExpressionTransformerFacade trxConsequence2DroolsRewardExpressionTransformerFacade) {
        super(trxConsequence2DroolsRewardExpressionTransformerFacade);
    }

    @Override
    protected String initiativeTrxConsequenceRuleBuild(String initiativeId, String organizationId, String ruleName, TrxCountDTO trxConsequence) {
        return """
                                
                rule "%s-%s"
                salience %d
                agenda-group "%s"
                when
                   $trx: %s()
                   eval(java.util.List.of("%s").equals($trx.getInitiativeRejectionReasons().get("%s")))
                then %s
                end
                """.formatted(
                ruleName,
                getTrxConsequenceRuleName(),
                getTrxConsequenceRuleOrder(),
                initiativeId,
                TransactionDroolsDTO.class.getName(),
                RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason(),
                initiativeId,
                buildConsequences(
                        initiativeId,
                        organizationId,
                        trxConsequence
                )
        );
    }

    @Override
    protected String getTrxConsequenceRuleName() {
        return "TRXCOUNT-REWARD";
    }

    @Override
    protected int getTrxConsequenceRuleOrder() {
        return RewardConstants.INITIATIVE_TRX_CONSEQUENCE_TRX_COUNT_ORDER;
    }
}
