package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.test.fakers.rule.ThresholdDTOFaker;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

class ThresholdTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<ThresholdDTO> {

    private final ThresholdTrxCondition2DroolsRuleTransformer transformer = new ThresholdTrxCondition2DroolsRuleTransformer(new TrxCondition2DroolsConditionTransformerFacadeImpl());
    private final ThresholdDTO thresholdRule = ThresholdDTOFaker.mockInstance(0);

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
                salience 3
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("agendaGroup", new it.gov.pagopa.reward.model.counters.InitiativeCounters())
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("agendaGroup") == null, !(amount >= new java.math.BigDecimal("0") && amount <= new java.math.BigDecimal("10")))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("agendaGroup",k->new java.util.ArrayList<>()).add("TRX_RULE_THRESHOLD_FAIL");
                end
                """;
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getSuccessfulUseCaseSuppliers() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setAmount(BigDecimal.valueOf(10.00));
        return List.of(() -> trx);
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getFailingUseCaseSuppliers() {
        TransactionDroolsDTO trx1 = new TransactionDroolsDTO();
        trx1.setAmount(BigDecimal.valueOf(10.01));

        TransactionDroolsDTO trx2 = new TransactionDroolsDTO();
        trx2.setAmount(BigDecimal.valueOf(-0.01));
        return List.of(() -> trx1, () -> trx2);
    }

    @Override
    protected String toUseCase(TransactionDroolsDTO trx) {
        return trx.getAmount().toString();
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.THRESHOLD.getRejectionReason();
    }
}
