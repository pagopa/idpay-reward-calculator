package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.test.fakers.rule.TrxCountDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.util.List;
import java.util.function.Supplier;

class TrxCountTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<TrxCountDTO> {

    private final TrxCountTrxCondition2DroolsRuleTransformer transformer = new TrxCountTrxCondition2DroolsRuleTransformer(new TrxCondition2DroolsConditionTransformerFacadeImpl());
    private final TrxCountDTO thresholdRule = TrxCountDTOFaker.mockInstance(0);

    @Override
    protected TrxCountTrxCondition2DroolsRuleTransformer getTransformer() {
        return transformer;
    }

    @Override
    protected TrxCountDTO getInitiativeTrxCondition() {
        return thresholdRule;
    }

    @Override
    protected String getExpectedRule() {
        return """
                                
                rule "ruleName-TRXCOUNT"
                salience 5
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("agendaGroup", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "agendaGroup"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("agendaGroup") == null, !($userInitiativeCounters.trxNumber >= new java.lang.Long("-1") && $userInitiativeCounters.trxNumber <= new java.lang.Long("9")))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("agendaGroup",k->new java.util.ArrayList<>()).add("TRX_RULE_TRXCOUNT_FAIL");
                end
                """;
    }

    Long startingTransactioCount = null;

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getSuccessfulUseCaseSuppliers() {
        startingTransactioCount = null;
        return List.of(TransactionDroolsDTO::new);
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getFailingUseCaseSuppliers() {
        startingTransactioCount = 11L;
        return List.of(TransactionDroolsDTO::new);
    }

    @Override
    protected UserInitiativeCounters getInitiativeCounters() {
        if(startingTransactioCount!=null) {
            final UserInitiativeCounters counters = new UserInitiativeCounters("USERID", InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,"agendaGroup");
            counters.setTrxNumber(startingTransactioCount);
            return counters;
        } else {
            return null;
        }
    }

    @Override
    protected String toUseCase(TransactionDroolsDTO trx) {
        return "trxNumber: " + startingTransactioCount;
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason();
    }
}
