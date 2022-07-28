package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class InitiativeTrxConsequence2DroolsRuleTransformerTest<T extends InitiativeTrxConsequence> {

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
        ((Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl")).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger("org.drools.compiler.kie.builder.impl")).setLevel(Level.WARN);
    }

    protected BigDecimal bigDecimalValue(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.UNNECESSARY);
    }

    protected abstract InitiativeTrxConsequence2DroolsRuleTransformer<T> getTransformer();
    protected abstract T getInitiativeTrxConsequence();
    protected abstract String getExpectedRule();
    protected abstract RewardTransaction getTransaction();
    protected abstract BigDecimal getExpectedReward();

    @Test
    void testReward(){
        String rule = getTransformer().apply("agendaGroup", "ruleName", getInitiativeTrxConsequence());

        Assertions.assertEquals(getExpectedRule(), rule);

        RewardTransaction trx = getTransaction();

        testRule(rule, trx, getExpectedReward());
    }

    @Test
    void testDiscardedIfRejected(){
        String rule = getTransformer().apply("agendaGroup", "ruleName", getInitiativeTrxConsequence());

        Assertions.assertEquals(getExpectedRule(), rule);

        RewardTransaction trx = getTransaction();
        trx.setRejectionReason(List.of("REJECTION"));

        testRule(rule, trx, null);
    }

    private final Map<String, BigDecimal> dummyReward = Map.of("DUMMYINITIATIVE", BigDecimal.TEN);

    protected void testRule(String rule, RewardTransaction trx, BigDecimal expectReward){
        trx.setRewards(new HashMap<>());
        trx.getRewards().putAll(dummyReward);
        KieContainer kieContainer = buildRule(rule);
        executeRule(trx, kieContainer);
        Assertions.assertEquals(BigDecimal.TEN, trx.getRewards().get("DUMMYINITIATIVE"));
        Assertions.assertEquals(
                expectReward
                , trx.getRewards().get("agendaGroup"));
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

    protected void executeRule(RewardTransaction trx, KieContainer kieContainer){
        @SuppressWarnings("unchecked")
        List<Command<?>> commands = Arrays.asList(
                CommandFactory.newInsert(trx),
                new AgendaGroupSetFocusCommand("agendaGroup")
        );
        kieContainer.newStatelessKieSession().execute(CommandFactory.newBatchExecution(commands));
    }
}
