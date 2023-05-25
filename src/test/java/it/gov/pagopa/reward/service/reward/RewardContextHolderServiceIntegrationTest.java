package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.config.EmbeddedRedisTestConfiguration;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.connector.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.reward.service.reward.evaluate.RuleEngineService;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestPropertySource(
        properties = {
                "spring.redis.enabled=true",
        }
)
@ContextConfiguration(classes = EmbeddedRedisTestConfiguration.class)
class RewardContextHolderServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KieContainerBuilderService kieContainerBuilderService;
    @Autowired
    private ReactiveRedisTemplate<String, byte[]> reactiveRedisTemplate;
    @Autowired
    private RewardContextHolderServiceImpl rewardContextHolderService;
    @Autowired
    private RuleEngineService ruleEngineService;

    @Test
    void testKieBuildWithRedisCache() {

        // Assert the starting built rule size is 0
        int startingRuleBuiltSize = RewardRuleConsumerConfigTest.getRuleBuiltSize(rewardContextHolderService);

        Assertions.assertEquals(0, startingRuleBuiltSize);

        // Caching invalid rules
        reactiveRedisTemplate.opsForValue().set(RewardContextHolderServiceImpl.REWARD_CONTEXT_HOLDER_CACHE_NAME, "INVALIDOBJECT".getBytes()).block();
        refreshAndAssertKieContainerRuleSize(0);

        // Build a valid KieBase that produces a rule of size 1, assert KieBase is null after Reflection
        DroolsRule dr = new DroolsRule();
        dr.setId("NAME");
        dr.setName("INITIATIVE1");
        dr.setRule("""
                package %s;
                
                import java.util.List;
                                
                rule "%s"
                agenda-group "%s"
                when $trx: %s()
                then $trx.setRejectionReasons(List.of("OK"));
                end
                """.formatted(
                        KieContainerBuilderServiceImpl.RULES_BUILT_PACKAGE,
                        dr.getRule(),
                        dr.getName(),
                        TransactionDTO.class.getName()
                )
        );

        buildAndCacheRules(List.of(dr));

        setContextHolderFieldToNull("kieBase");
        setContextHolderFieldToNull("kieBaseSerialized");

        Assertions.assertNull(rewardContextHolderService.getRewardRulesKieBase());

        // Refresh KieBase and assert the built rules has expected size
        refreshAndAssertKieContainerRuleSize(1);

        // Execute rule and assert transaction has the expected rejection reason
        TransactionDTO trxMock = TransactionDTOFaker.mockInstance(1);
        RewardTransactionDTO result = executeRules(trxMock);

        Assertions.assertEquals(List.of("OK"), result.getRejectionReasons());

        // Set a null kieBase
        rewardContextHolderService.setRewardRulesKieBase(null);
        TestUtils.waitFor(
                ()->(reactiveRedisTemplate.opsForValue().get(RewardContextHolderServiceImpl.REWARD_CONTEXT_HOLDER_CACHE_NAME).block()) == null,
                ()->"KieBase not saved in cache",
                10,
                500
        );

        rewardContextHolderService.refreshKieContainer();
        TestUtils.waitFor(
                ()-> (reactiveRedisTemplate.opsForValue().get(RewardContextHolderServiceImpl.REWARD_CONTEXT_HOLDER_CACHE_NAME).block()) != null,
                ()-> "KieBase is null",
                10,
                500
        );

        KieBase resultKieBase = rewardContextHolderService.getRewardRulesKieBase();
        Assertions.assertNotNull(resultKieBase);

        int resultRuleBuiltSize = RewardRuleConsumerConfigTest.getRuleBuiltSize(rewardContextHolderService);
        Assertions.assertEquals(0, resultRuleBuiltSize);
    }

    private void refreshAndAssertKieContainerRuleSize(int expected) {
        rewardContextHolderService.refreshKieContainer();
        TestUtils.waitFor(
                ()->rewardContextHolderService.getRewardRulesKieBase() != null,
                ()->"KieBase is null",
                10,
                500
        );
        int ruleBuiltSize = RewardRuleConsumerConfigTest.getRuleBuiltSize(rewardContextHolderService);
        Assertions.assertEquals(expected, ruleBuiltSize);
    }

    private void buildAndCacheRules(Collection<DroolsRule> drs) {
        KieBase kieBase = kieContainerBuilderService.build(Flux.fromIterable(drs)).block();
        rewardContextHolderService.setRewardRulesKieBase(kieBase);
        TestUtils.waitFor(
                ()->(reactiveRedisTemplate.opsForValue().get(RewardContextHolderServiceImpl.REWARD_CONTEXT_HOLDER_CACHE_NAME).block()) != null,
                ()->"KieBase not saved in cache",
                10,
                500
        );
    }

    private void setContextHolderFieldToNull(String fieldName) {
        Field field = ReflectionUtils.findField(RewardContextHolderServiceImpl.class, fieldName);
        Assertions.assertNotNull(field);
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, rewardContextHolderService, null);
    }

    private RewardTransactionDTO executeRules(TransactionDTO trx) {

        List<String> initiatives = List.of("INITIATIVE1");
        UserInitiativeCountersWrapper counters = UserInitiativeCountersWrapper.builder()
                .userId("USER1")
                .initiatives(
                        new HashMap<>(Map.of(
                                "INITIATIVE1",
                                new UserInitiativeCounters(trx.getUserId(), "INITIATIVE1")
                        ))
                )
                .build();

        return ruleEngineService.applyRules(trx, initiatives, counters);
    }

}
