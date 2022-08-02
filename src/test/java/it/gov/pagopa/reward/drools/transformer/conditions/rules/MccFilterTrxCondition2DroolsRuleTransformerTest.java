package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;
import it.gov.pagopa.reward.test.fakers.rule.MccFilterDTOFaker;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.util.List;
import java.util.function.Supplier;

class MccFilterTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<MccFilterDTO> {

    private final MccFilterTrxCondition2DroolsRuleTransformer transformer = new MccFilterTrxCondition2DroolsRuleTransformer(new TrxCondition2DroolsConditionTransformerFacadeImpl());
    private final MccFilterDTO mccFilterRule = MccFilterDTOFaker.mockInstance(0);

    @Override
    protected MccFilterTrxCondition2DroolsRuleTransformer getTransformer() {
        return transformer;
    }

    @Override
    protected MccFilterDTO getInitiativeTrxCondition() {
        return mccFilterRule;
    }

    @Override
    protected String getExpectedRule() {
        return """
                                
                rule "ruleName-MCCFILTER"
                salience 6
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("agendaGroup", new it.gov.pagopa.reward.model.counters.InitiativeCounters())
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("agendaGroup") == null, !(mcc not in ("0897","MCC_0")))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("agendaGroup",k->new java.util.ArrayList<>()).add("TRX_RULE_MCCFILTER_FAIL");
                end
                """;
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getSuccessfulUseCaseSuppliers() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setMcc("ALLOWEDMCC");
        return List.of(() -> trx);
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getFailingUseCaseSuppliers() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setMcc("MCC_0");
        return List.of(() -> trx);
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.MCCFILTER.getRejectionReason();
    }
}
