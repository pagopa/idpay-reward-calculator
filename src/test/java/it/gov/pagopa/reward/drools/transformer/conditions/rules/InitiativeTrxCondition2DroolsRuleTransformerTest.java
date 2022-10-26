package it.gov.pagopa.reward.drools.transformer.conditions.rules;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImplTest;
import it.gov.pagopa.reward.service.build.RewardRule2DroolsRuleServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieContainer;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Supplier;

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

    protected abstract List<Supplier<TransactionDroolsDTO>> getSuccessfulUseCaseSuppliers();

    protected abstract List<Supplier<TransactionDroolsDTO>> getFailingUseCaseSuppliers();

    protected abstract String getExpectedRejectionReason();

    @Test
    void testNoRejectionReason() {
        for(Supplier<TransactionDroolsDTO> trxSupplier : getSuccessfulUseCaseSuppliers()) {
            trxSupplier.get(); // executed in order to configure useCase

            String rule = getTransformer().apply("agendaGroup", "ruleName", getInitiativeTrxCondition());
            Assertions.assertEquals(getExpectedRule(), rule);

            testRule(rule, trxSupplier, true, false, false);
            testRule(rule, trxSupplier, false, false, false);

            // short-circuited
            testRule(rule, trxSupplier, true, false, true);
            testRule(rule, trxSupplier, false, false, true);
        }
    }

    @Test
    void testRejectionReason() {
        for(Supplier<TransactionDroolsDTO> trxSupplier : getFailingUseCaseSuppliers()) {
            trxSupplier.get(); // executed in order to configure useCase

            String rule = getTransformer().apply("agendaGroup", "ruleName", getInitiativeTrxCondition());
            Assertions.assertEquals(getExpectedRule(), rule);

            testRule(rule, trxSupplier, true, true, false);
            testRule(rule, trxSupplier, false, true, false);

            // short-circuited
            testRule(rule, trxSupplier, true, true, true);
            testRule(rule, trxSupplier, false, true, true);
        }
    }

    private final List<String> dummyRejection = List.of("DUMMYREJECTION");
    private final Map<String, List<String>> dummyRejectionMap = Map.of("agendaGroup", dummyRejection);

    protected TransactionDroolsDTO testRule(String rule, Supplier<TransactionDroolsDTO> trxSupplier, boolean simulateOtherRejection, boolean expectRejectionReason, boolean shortCircuited) {
        TransactionDroolsDTO trx = trxSupplier.get(); // executed in order to reset useCase condition at each execution

        trx.setInitiativeRejectionReasons(new HashMap<>());
        Map<String, List<String>> expectedInitiativeRejectionReasons = expectRejectionReason ? Map.of("agendaGroup", List.of(getExpectedRejectionReason())) : Collections.emptyMap();

        if (simulateOtherRejection) {
            trx.getInitiativeRejectionReasons().put("agendaGroup", new ArrayList<>(dummyRejection));

            expectedInitiativeRejectionReasons = expectRejectionReason && !shortCircuited ? Map.of("agendaGroup", List.of("DUMMYREJECTION", getExpectedRejectionReason())) : dummyRejectionMap;
        }
        KieBase kieBase = buildRule(rule);
        executeRule(trx, shortCircuited, kieBase);
        Assertions.assertEquals(
                expectedInitiativeRejectionReasons
                , trx.getInitiativeRejectionReasons()
                , "Failing useCase: %s".formatted(toUseCase(trx)));

        return trx;
    }

    protected String toUseCase(TransactionDroolsDTO trx){
        return trx.toString();
    }

    protected KieBase buildRule(String rule) {
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

    protected void executeRule(TransactionDroolsDTO trx, boolean shortCircuited, KieBase kieBase) {
        UserInitiativeCounters counters = new UserInitiativeCounters("userId", new HashMap<>());
        InitiativeCounters initiativeCounters = getInitiativeCounters();
        if(initiativeCounters!=null){
            counters.getInitiatives().put("agendaGroup", initiativeCounters);
        }

        RewardRule2DroolsRuleServiceTest.executeRule("agendaGroup", trx, shortCircuited, counters, kieBase);
    }

    protected InitiativeCounters getInitiativeCounters(){
        return null;
    }
}
