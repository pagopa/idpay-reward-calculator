package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.config.EmbeddedRedisTestConfiguration;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.model.DroolsRule;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.build.KieContainerBuilderService;
import it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl;
import it.gov.pagopa.reward.service.reward.evaluate.RuleEngineService;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
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
    private ApplicationEventPublisher applicationEventPublisher;
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

        KieBase kieBase = kieContainerBuilderService.build(Flux.just(dr)).block();
        rewardContextHolderService.setRewardRulesKieBase(kieBase);
        waitFor(
                ()->(reactiveRedisTemplate.opsForValue().get(RewardContextHolderServiceImpl.REWARD_CONTEXT_HOLDER_CACHE_NAME).block()) != null,
                ()->"KieBase not saved in cache",
                10,
                500
        );

        Field kieBaseField = ReflectionUtils.findField(RewardContextHolderServiceImpl.class, "kieBase");
        Assertions.assertNotNull(kieBaseField);
        ReflectionUtils.makeAccessible(kieBaseField);
        ReflectionUtils.setField(kieBaseField, rewardContextHolderService, null);

        Assertions.assertNull(rewardContextHolderService.getRewardRulesKieBase());

        // Refresh KieBase and assert the built rules has expected size
        rewardContextHolderService.refreshKieContainer();
        waitFor(
                ()->rewardContextHolderService.getRewardRulesKieBase() != null,
                ()->"KieBase is null",
                10,
                500
        );

        int ruleBuiltSize = RewardRuleConsumerConfigTest.getRuleBuiltSize(rewardContextHolderService);
        Assertions.assertEquals(1, ruleBuiltSize);

        // Execute rule and assert transaction has the expected rejection reason
        TransactionDTO trxMock = TransactionDTOFaker.mockInstance(1);
        RewardTransactionDTO result = executeRules(trxMock);

        Assertions.assertEquals(List.of("OK"), result.getRejectionReasons());

        // Set a null kieBase
        rewardContextHolderService.setRewardRulesKieBase(null);
        waitFor(
                ()->(reactiveRedisTemplate.opsForValue().get(RewardContextHolderServiceImpl.REWARD_CONTEXT_HOLDER_CACHE_NAME).block()) == null,
                ()->"KieBase not saved in cache",
                10,
                500
        );

        rewardContextHolderService.refreshKieContainer();
        waitFor(
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

    private RewardTransactionDTO executeRules(TransactionDTO trx) {

        List<String> initiatives = List.of("INITIATIVE1");
        UserInitiativeCounters counters = UserInitiativeCounters.builder()
                .userId("USER1")
                .initiatives(
                        new HashMap<>(Map.of(
                                "INITIATIVE1",
                                new InitiativeCounters("INITIATIVE1")
                        ))
                )
                .build();

        return ruleEngineService.applyRules(trx, initiatives, counters);
    }

}
