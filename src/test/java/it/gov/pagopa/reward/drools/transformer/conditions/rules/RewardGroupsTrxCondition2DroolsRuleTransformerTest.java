package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTOFaker;
import it.gov.pagopa.reward.model.RewardTransaction;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.math.BigDecimal;

public class RewardGroupsTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<RewardGroupsDTO> {

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
                                
                rule "ruleName-REWARDGROUP"
                salience 2
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(((amount >= new java.math.BigDecimal("0") && amount <= new java.math.BigDecimal("5")))))
                then $trx.getRejectionReason().add("TRX_RULE_REWARDGROUP_FAIL");
                end
                """;
    }

    @Override
    protected RewardTransaction getSuccessfulUseCase() {
        RewardTransaction trx = new RewardTransaction();
        trx.setAmount(BigDecimal.valueOf(5));
        return trx;
    }

    @Override
    protected RewardTransaction getFailingUseCase() {
        RewardTransaction trx = new RewardTransaction();
        trx.setAmount(BigDecimal.valueOf(-0.01));
        return trx;
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.REWARDGROUP.getRejectionReason();
    }
}
