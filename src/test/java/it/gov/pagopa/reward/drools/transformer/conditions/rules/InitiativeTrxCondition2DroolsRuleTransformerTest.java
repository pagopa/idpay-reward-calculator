package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.config.RuleEngineConfig;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImplTest;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.command.CommandFactory;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public abstract class InitiativeTrxCondition2DroolsRuleTransformerTest<T extends InitiativeTrxCondition> {

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
        ((Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl")).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger("org.drools.compiler.kie.builder.impl")).setLevel(Level.WARN);
    }

    protected BigDecimal bigDecimalValue(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.UNNECESSARY);
    }

    protected abstract InitiativeTrxCondition2DroolsRuleTransformer<T> getTransformer();

    protected abstract T getInitiativeTrxCondition();

    protected abstract String getExpectedRule();

    protected abstract TransactionDroolsDTO getSuccessfulUseCase();

    protected abstract TransactionDroolsDTO getFailingUseCase();

    protected abstract String getExpectedRejectionReason();

    @Test
    void testNoRejectionReason() {
        String rule = getTransformer().apply("agendaGroup", "ruleName", getInitiativeTrxCondition());

        Assertions.assertEquals(getExpectedRule(), rule);

        TransactionDroolsDTO trx = getSuccessfulUseCase();

        testRule(rule, trx, true, false, false);
        testRule(rule, trx, false, false, false);

        // short-circuited
        testRule(rule, trx, true, false, true);
        testRule(rule, trx, false, false, true);
    }

    @Test
    void testRejectionReason() {
        String rule = getTransformer().apply("agendaGroup", "ruleName", getInitiativeTrxCondition());

        Assertions.assertEquals(getExpectedRule(), rule);

        TransactionDroolsDTO trx = getFailingUseCase();

        testRule(rule, trx, true, true, false);
        testRule(rule, trx, false, true, false);

        // short-circuited
        testRule(rule, trx, true, true, true);
        testRule(rule, trx, false, true, true);
    }

    private final Map<String, List<String>> dummyRejection = Map.of("agendaGroup", new ArrayList<>(List.of("DUMMYREJECTION")));

    protected void testRule(String rule, TransactionDroolsDTO trx, boolean simulateOtherRejection, boolean expectRejectionReason, boolean shortCircuited) {
        trx.setInitiativeRejectionReasons(new HashMap<>());
        Map<String, List<String>> expectedInitiativeRejectionReasons = expectRejectionReason ? Map.of("agendaGroup", List.of(getExpectedRejectionReason())) : Collections.emptyMap();

        if (simulateOtherRejection) {
            trx.getInitiativeRejectionReasons().putAll(dummyRejection);

            expectedInitiativeRejectionReasons = expectRejectionReason && !shortCircuited ? Map.of("agendaGroup", List.of("DUMMYREJECTION", getExpectedRejectionReason())) : dummyRejection;
        }
        KieContainer kieContainer = buildRule(rule);
        executeRule(trx, shortCircuited, kieContainer);
        Assertions.assertEquals(
                expectedInitiativeRejectionReasons
                , trx.getInitiativeRejectionReasons());
    }

    protected KieContainer buildRule(String rule) {
        DroolsRule dr = new DroolsRule();
        dr.setId("agendaGroup");
        dr.setName("ruleName");
        dr.setRule("""
                package %s;
                                
                %s
                """.formatted(
                KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE,
                rule));

        try {
            return new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(dr)).block();
        } catch (RuntimeException e) {
            System.out.printf("Something gone wrong building the rule: %s%n", dr.getRule());
            throw e;
        }
    }

    protected void executeRule(TransactionDroolsDTO trx, boolean shortCircuited, KieContainer kieContainer) {
        RuleEngineConfig ruleEngineConfig = new RuleEngineConfig();
        ruleEngineConfig.setShortCircuitConditions(shortCircuited);
        @SuppressWarnings("unchecked")
        List<Command<?>> commands = Arrays.asList(
                CommandFactory.newInsert(ruleEngineConfig),
                CommandFactory.newInsert(trx),
                new AgendaGroupSetFocusCommand("agendaGroup")
        );
        kieContainer.newStatelessKieSession().execute(CommandFactory.newBatchExecution(commands));
    }
}
