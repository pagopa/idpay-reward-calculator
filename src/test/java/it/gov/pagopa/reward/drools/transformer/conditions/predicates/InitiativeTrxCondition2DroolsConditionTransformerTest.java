package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImplTest;
import org.drools.core.command.runtime.rule.AgendaGroupSetFocusCommand;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.kie.api.KieBase;
import org.kie.api.command.Command;
import org.kie.internal.command.CommandFactory;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

abstract class InitiativeTrxCondition2DroolsConditionTransformerTest {

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
        ((Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl")).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger("org.drools.compiler.kie.builder.impl")).setLevel(Level.WARN);
    }

    protected BigDecimal bigDecimalValue(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.UNNECESSARY);
    }

    protected void testRule(String testName, String rewardCondition, TransactionDroolsDTO trx, boolean expectTrueCondition){
        trx.setInitiatives(new ArrayList<>());
        KieBase kieBase = buildRule(testName, rewardCondition);
        executeRule(trx, testName, kieBase);
        Assertions.assertEquals(
                expectTrueCondition ? List.of(testName) : Collections.emptyList()
                , trx.getInitiatives());
    }

    protected KieBase buildRule(String testName, String rewardCondition) {
        DroolsRule dr = new DroolsRule();
        dr.setId(testName);
        dr.setName("RULE");
        dr.setRule("""
                package %s;
                                
                rule "%s"
                agenda-group "%s"
                when %s
                then $trx.getInitiatives().add("%s");
                end
                """.formatted(
                KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE,
                dr.getName(),
                dr.getId(),
                buildCondition(rewardCondition),
                testName));
        dr.setUpdateDate(LocalDateTime.now());

        try{
            return new KieContainerBuilderServiceImpl(Mockito.mock(DroolsRuleRepository.class)).build(Flux.just(dr)).block();
        } catch (RuntimeException e){
            System.out.printf("Something gone wrong building the rule: %s%n", dr.getRule());
            throw e;
        }
    }

    protected String buildCondition(String rewardCondition) {
        return """
                %s$trx: %s(%s)""".formatted(
                getInitiativeCounters()!=null? "$userInitiativeCounters: %s()\n".formatted(UserInitiativeCounters.class.getName()) : "",
                TransactionDroolsDTO.class.getName(),
                rewardCondition
        );
    }

    protected void executeRule(TransactionDroolsDTO trx, String agendaGroup, KieBase kieBase){
        List<Command<?>> commands = buildKieContainerCommands(trx, agendaGroup);
        kieBase.newStatelessKieSession().execute(CommandFactory.newBatchExecution(commands));
    }

    protected List<Command<?>> buildKieContainerCommands(TransactionDroolsDTO trx, String agendaGroup) {

        final UserInitiativeCounters counter = getInitiativeCounters();
        @SuppressWarnings("unchecked")
        List<Command<?>> commands = Arrays.asList(
                CommandFactory.newInsert(trx),
                CommandFactory.newInsert(counter),
                new AgendaGroupSetFocusCommand(agendaGroup)
        );
        return commands;
    }

    protected UserInitiativeCounters getInitiativeCounters() {
        return null;
    }
}
