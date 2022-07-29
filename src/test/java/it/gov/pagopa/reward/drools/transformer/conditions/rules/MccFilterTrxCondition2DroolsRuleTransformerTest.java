package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;
import it.gov.pagopa.reward.test.fakers.rule.MccFilterDTOFaker;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

class MccFilterTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<MccFilterDTO> {

    private final MccFilterTrxCondition2DroolsRuleTransformer transformer = new MccFilterTrxCondition2DroolsRuleTransformer(new TrxCondition2DroolsConditionTransformerFacadeImpl());
    public final MccFilterDTO mccFilterRule = MccFilterDTOFaker.mockInstance(0);

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
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("agendaGroup") == null, !(mcc not in ("0897","MCC_0")))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("agendaGroup",k->new java.util.ArrayList<>()).add("TRX_RULE_MCCFILTER_FAIL");
                end
                """;
    }

    @Override
    protected TransactionDroolsDTO getSuccessfulUseCase() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setMcc("ALLOWEDMCC");
        return trx;
    }

    @Override
    protected TransactionDroolsDTO getFailingUseCase() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setMcc("MCC_0");
        return trx;
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.MCCFILTER.getRejectionReason();
    }
}
