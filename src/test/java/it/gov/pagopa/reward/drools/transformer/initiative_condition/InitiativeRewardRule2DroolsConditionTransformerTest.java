package it.gov.pagopa.reward.drools.transformer.initiative_condition;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.RewardTransaction;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImplTest;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.Collections;
import java.util.List;

public abstract class InitiativeRewardRule2DroolsConditionTransformerTest {

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
        ((Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl")).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger("org.drools.compiler.kie.builder.impl")).setLevel(Level.WARN);
    }

    protected BigDecimal bigDecimalValue(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.UNNECESSARY);
    }

    protected void testRule(String testName, String rewardCondition, RewardTransaction trx, boolean expectTrueCondition){
        trx.setInitiatives(new ArrayList<>());
        KieContainer kieContainer = buildRule(testName, rewardCondition);
        executeRule(trx, testName, kieContainer);
        Assertions.assertEquals(
                expectTrueCondition ? List.of(testName) : Collections.emptyList()
                , trx.getInitiatives());
    }

    protected KieContainer buildRule(String testName, String rewardCondition) {
        DroolsRule dr = new DroolsRule();
        dr.setId(testName);
        dr.setName("RULE");
        dr.setRule("""
                package dummy;
                                
                rule "%s"
                agenda-group "%s"
                when $trx: %s(%s)
                then $trx.getInitiatives().add("%s");
                end
                """.formatted(
                dr.getName(),
                dr.getId(),
                RewardTransaction.class.getName(),
                rewardCondition,
                testName));

        try{
            return new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(dr)).block();
        } catch (RuntimeException e){
            System.out.printf("Something gone wrong building the rule: %s%n", dr.getRule());
            throw e;
        }
    }

    protected void executeRule(RewardTransaction trx, String agendaGroup, KieContainer kieContainer){
        @SuppressWarnings("unchecked")
        List<Command<?>> commands = Arrays.asList(
                CommandFactory.newInsert(trx),
                new AgendaGroupSetFocusCommand(agendaGroup)
        );
        kieContainer.newStatelessKieSession().execute(CommandFactory.newBatchExecution(commands));
    }
}
