package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import it.gov.pagopa.reward.drools.transformer.conditions.TrxCondition2DroolsConditionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.test.fakers.rule.RewardGroupsDTOFaker;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.util.List;
import java.util.function.Supplier;

class RewardGroupsTrxCondition2DroolsRuleTransformerTest extends InitiativeTrxCondition2DroolsRuleTransformerTest<RewardGroupsDTO> {

    private final RewardGroupsTrxCondition2DroolsRuleTransformer transformer = new RewardGroupsTrxCondition2DroolsRuleTransformer(new TrxCondition2DroolsConditionTransformerFacadeImpl());
    private final RewardGroupsDTO RewardGroupsRule = RewardGroupsDTOFaker.mockInstance(0);

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
                salience 2
                agenda-group "agendaGroup"
                when
                   $config: it.gov.pagopa.reward.config.RuleEngineConfig()
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
                   $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("agendaGroup", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "agendaGroup"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(!$config.shortCircuitConditions || initiativeRejectionReasons.get("agendaGroup") == null, !(((effectiveAmountCents >= new java.lang.Long("0") && effectiveAmountCents <= new java.lang.Long("500")))))
                then $trx.getInitiativeRejectionReasons().computeIfAbsent("agendaGroup",k->new java.util.ArrayList<>()).add("TRX_RULE_REWARDGROUPS_CONDITION_FAIL");
                end
                """;
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getSuccessfulUseCaseSuppliers() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setEffectiveAmountCents(5_00L);
        return List.of(() -> trx);
    }

    @Override
    protected List<Supplier<TransactionDroolsDTO>> getFailingUseCaseSuppliers() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setEffectiveAmountCents(-1L);
        return List.of(() -> trx);
    }

    @Override
    protected String getExpectedRejectionReason() {
        return RewardConstants.InitiativeTrxConditionOrder.REWARDGROUPS_CONDITION.getRejectionReason();
    }
}
