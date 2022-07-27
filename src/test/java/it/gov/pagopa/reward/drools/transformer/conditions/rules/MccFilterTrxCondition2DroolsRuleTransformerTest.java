package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;
import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTOFaker;
import it.gov.pagopa.reward.model.RewardTransaction;
import it.gov.pagopa.reward.utils.RewardConstants;

public class MccFilterTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<MccFilterDTO> {

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
                   $trx: it.gov.pagopa.reward.model.RewardTransaction(!$config.shortCircuitConditions || rejectionReason.size() == 0, !(mcc not in ("0897","MCC_0")))
                then $trx.getRejectionReason().add("TRX_RULE_MCCFILTER_FAIL");
                end
                """;
    }

    @Override
    protected RewardTransaction getSuccessfulUseCase() {
        RewardTransaction trx = new RewardTransaction();
        trx.setMcc("ALLOWEDMCC");
        return trx;
    }

    @Override
    protected RewardTransaction getFailingUseCase() {
        RewardTransaction trx = new RewardTransaction();
        trx.setMcc("MCC_0");
        return trx;
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.MCCFILTER.getRejectionReason();
    }
}
