package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTOFaker;
import it.gov.pagopa.reward.model.RewardTransaction;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.math.BigDecimal;

public class ThresholdTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<ThresholdDTO> {

    private final ThresholdTrxCondition2DroolsRuleTransformer transformer = new ThresholdTrxCondition2DroolsRuleTransformer(new TrxCondition2DroolsConditionTransformerFacadeImpl());
    public final ThresholdDTO thresholdRule = ThresholdDTOFaker.mockInstance(0);

    @Override
    protected ThresholdTrxCondition2DroolsRuleTransformer getTransformer() {
        return transformer;
    }

    @Override
    protected ThresholdDTO getInitiativeTrxCondition() {
        return thresholdRule;
    }

    @Override
    protected String getExpectedRule() {
        return """
                                
                rule "ruleName-THRESHOLD"
                salience 1
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(amount >= new java.math.BigDecimal("0") && amount <= new java.math.BigDecimal("10")))
                then $trx.getRejectionReason().add("TRX_RULE_THRESHOLD_FAIL");
                end
                """;
    }

    @Override
    protected RewardTransaction getSuccessfulUseCase() {
        RewardTransaction trx = new RewardTransaction();
        trx.setAmount(BigDecimal.valueOf(10.00));
        return trx;
    }

    @Override
    protected RewardTransaction getFailingUseCase() {
        RewardTransaction trx = new RewardTransaction();
        trx.setAmount(BigDecimal.valueOf(10.01));
        return trx;
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.THRESHOLD.getRejectionReason();
    }
}
