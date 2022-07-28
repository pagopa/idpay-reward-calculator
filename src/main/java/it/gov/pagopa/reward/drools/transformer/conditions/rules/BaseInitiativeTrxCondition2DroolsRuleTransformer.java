package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.config.RuleEngineConfig;
import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacade;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

public abstract class BaseInitiativeTrxCondition2DroolsRuleTransformer<T extends InitiativeTrxCondition> implements InitiativeTrxCondition2DroolsRuleTransformer<T> {

    private final TrxCondition2DroolsConditionTransformerFacade trxCondition2DroolsConditionTransformerFacade;

    protected BaseInitiativeTrxCondition2DroolsRuleTransformer(TrxCondition2DroolsConditionTransformerFacade trxCondition2DroolsConditionTransformerFacade) {
        this.trxCondition2DroolsConditionTransformerFacade = trxCondition2DroolsConditionTransformerFacade;
    }

    protected abstract RewardConstants.InitiativeTrxConditionOrder getTrxConditionOrder();

    @Override
    public String apply(String agendaGroup, String ruleNamePrefix, T trxCondition) {
        return initiativeTrxConditionBuild(
                agendaGroup,
                ruleNamePrefix,
                trxCondition
        );
    }

    protected String initiativeTrxConditionBuild(String initiativeId, String ruleName, T trxCondition) {
        return initiativeTrxConditionBuild(initiativeId, ruleName, trxCondition, getTrxConditionOrder().getRejectionReason());
    }

    protected String initiativeTrxConditionBuild(String initiativeId, String ruleName, T trxCondition, String rejectionReason) {
        return """
                                
                rule "%s-%s"
                salience %d
                agenda-group "%s"
                when
                   $config: %s()
                   $trx: %s(!$config.shortCircuitConditions || initiativeRejectionReasons.get("%s") == null, !(%s))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("%s",k->new java.util.ArrayList<>()).add("%s");
                end
                """.formatted(
                ruleName,
                getTrxConditionOrder().name(),
                RewardConstants.InitiativeTrxConditionOrder.values().length-getTrxConditionOrder().ordinal(),
                initiativeId,
                RuleEngineConfig.class.getName(),
                TransactionDroolsDTO.class.getName(),
                initiativeId,
                trxCondition2DroolsConditionTransformerFacade.apply(trxCondition),
                initiativeId,
                rejectionReason
        );
    }
}
