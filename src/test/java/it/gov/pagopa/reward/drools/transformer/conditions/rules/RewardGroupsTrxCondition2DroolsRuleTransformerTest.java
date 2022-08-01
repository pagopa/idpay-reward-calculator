package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.test.fakers.rule.RewardGroupsDTOFaker;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.math.BigDecimal;

class RewardGroupsTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<RewardGroupsDTO> {

    private final RewardGroupsTrxCondition2DroolsRuleTransformer transformer = new RewardGroupsTrxCondition2DroolsRuleTransformer(new TrxCondition2DroolsConditionTransformerFacadeImpl());
    public final RewardGroupsDTO RewardGroupsRule = RewardGroupsDTOFaker.mockInstance(0);

    @Override
    protected RewardGroupsTrxCondition2DroolsRuleTransformer getTransformer() {
        return transformer;
    }

    @Override
    protected RewardGroupsDTO getInitiativeTrxCondition() {
        return RewardGroupsRule;
    }

    @Override
    protected String getExpectedRule() {
        return """
                                
                rule "ruleName-REWARDGROUPS_CONDITION"
                salience 4
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("agendaGroup") == null, !(((amount >= new java.math.BigDecimal("0") && amount <= new java.math.BigDecimal("5")))))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("agendaGroup",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDGROUPS_CONDITION_FAIL");
                end
                """;
    }

    @Override
    protected TransactionDroolsDTO getSuccessfulUseCase() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setAmount(BigDecimal.valueOf(5));
        return trx;
    }

    @Override
    protected TransactionDroolsDTO getFailingUseCase() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setAmount(BigDecimal.valueOf(-0.01));
        return trx;
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.REWARDGROUPS_CONDITION.getRejectionReason();
    }
}
