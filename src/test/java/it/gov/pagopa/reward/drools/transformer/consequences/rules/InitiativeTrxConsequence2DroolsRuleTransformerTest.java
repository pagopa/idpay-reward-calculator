package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImplTest;
import it.gov.pagopa.reward.service.build.RewardRule2DroolsRuleServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class InitiativeTrxConsequence2DroolsRuleTransformerTest<T extends InitiativeTrxConsequence> {

    @BeforeAll
    public static void configDroolsLogLevel() {
        KieContainerBuilderServiceImplTest.configDroolsLogs();
        ((Logger) LoggerFactory.getLogger("it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl")).setLevel(Level.WARN);
        ((Logger) LoggerFactory.getLogger("org.drools.compiler.kie.builder.impl")).setLevel(Level.WARN);
    }

    protected BigDecimal bigDecimalValue(double value) {
        return toExpectedScale(BigDecimal.valueOf(value));
    }

    protected BigDecimal toExpectedScale(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    protected abstract InitiativeTrxConsequence2DroolsRuleTransformer<T> getTransformer();

    protected abstract T getInitiativeTrxConsequence();

    protected abstract String getExpectedRule();

    protected abstract TransactionDroolsDTO getTransaction();

    protected abstract BigDecimal getExpectedReward();

    @Test
    void testReward() {
        String rule = getTransformer().apply("initiativeId", "organizationId", "ruleName", getInitiativeTrxConsequence());

        Assertions.assertEquals(getExpectedRule(), rule);

        TransactionDroolsDTO trx = getTransaction();
        trx.setInitiativeRejectionReasons(Map.of("OTHERINITIATIVE", List.of("REJECTION")));

        testRule(rule, trx, getExpectedReward());
    }

    @Test
    void testDiscardedIfRejected() {
        String rule = getTransformer().apply("initiativeId", "organizationId", "ruleName", getInitiativeTrxConsequence());

        Assertions.assertEquals(getExpectedRule(), rule);

        TransactionDroolsDTO trx = getTransaction();
        trx.setInitiativeRejectionReasons(Map.of("initiativeId", List.of("REJECTION")));

        testRule(rule, trx, null);
    }

    private final Map<String, Reward> dummyReward = Map.of("DUMMYINITIATIVE", new Reward("DUMMYINITIATIVE", "DUMMYINITIATIVEORGANIZATION", BigDecimal.TEN, BigDecimal.TEN));

    protected TransactionDroolsDTO testRule(String rule, TransactionDroolsDTO trx, BigDecimal expectReward) {
        cleanRewards(trx);
        KieBase kieBase = buildRule(rule);
        executeRule(trx, kieBase);
        Assertions.assertEquals(dummyReward.get("DUMMYINITIATIVE"), trx.getRewards().get("DUMMYINITIATIVE"));
        Assertions.assertEquals(
                expectReward
                , Optional.ofNullable(trx.getRewards().get("initiativeId")).map(Reward::getAccruedReward).orElse(null));

        return trx;
    }

    protected void cleanRewards(TransactionDroolsDTO trx) {
        trx.setRewards(new HashMap<>());
        trx.getRewards().putAll(dummyReward);
    }

    protected KieBase buildRule(String rule) {
        DroolsRule dr = new DroolsRule();
        dr.setId("initiativeId");
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

    protected void executeRule(TransactionDroolsDTO trx, KieBase kieBase) {
        RewardRule2DroolsRuleServiceTest.executeRule("initiativeId", trx, false, getCounters(), kieBase);
    }

    protected UserInitiativeCountersWrapper getCounters() {
        return null;
    }
}
