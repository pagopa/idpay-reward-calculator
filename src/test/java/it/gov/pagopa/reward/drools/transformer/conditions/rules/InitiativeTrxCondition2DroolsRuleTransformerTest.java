package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.config.RuleEngineConfig;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.RewardTransaction;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    protected abstract RewardTransaction getSuccessfulUseCase();
    protected abstract RewardTransaction getFailingUseCase();
    protected abstract String getExpectedRejectionReason();

    @Test
    void testNoRejectionReason(){
        String rule = getTransformer().apply("agendaGroup", "ruleName", getInitiativeTrxCondition());

        Assertions.assertEquals(getExpectedRule(), rule);

        RewardTransaction trx = getSuccessfulUseCase();

        testRule(rule, trx, false, false);

        // short-circuited
        testRule(rule, trx, false, true);
    }

    @Test
    void testRejectionReason(){
        String rule = getTransformer().apply("agendaGroup", "ruleName", getInitiativeTrxCondition());

        Assertions.assertEquals(getExpectedRule(), rule);

        RewardTransaction trx = getFailingUseCase();

        testRule(rule, trx, true, false);

        // short-circuited
        testRule(rule, trx, true, true);
    }

    protected void testRule(String rule, RewardTransaction trx, boolean expectRejectionReason, boolean shortCircuited){
        trx.setRejectionReason(new ArrayList<>());
        trx.getRejectionReason().add("DUMMYREJECTION");
        KieContainer kieContainer = buildRule(rule);
        executeRule(trx, shortCircuited, kieContainer);
        Assertions.assertEquals(
                expectRejectionReason && !shortCircuited ? List.of("DUMMYREJECTION", getExpectedRejectionReason()) : List.of("DUMMYREJECTION")
                , trx.getRejectionReason());
    }

    protected KieContainer buildRule(String rule) {
        DroolsRule dr = new DroolsRule();
        dr.setId("agendaGroup");
        dr.setName("ruleName");
        dr.setRule("""
                package dummy;
                                
                %s
                """.formatted(rule));

        try{
            return new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(dr)).block();
        } catch (RuntimeException e){
            System.out.printf("Something gone wrong building the rule: %s%n", dr.getRule());
            throw e;
        }
    }

    protected void executeRule(RewardTransaction trx, boolean shortCircuited, KieContainer kieContainer){
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
