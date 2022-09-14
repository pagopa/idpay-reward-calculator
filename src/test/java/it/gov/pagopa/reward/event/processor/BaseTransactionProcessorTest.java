package it.gov.pagopa.reward.event.processor;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.reward.service.build.RewardRule2DroolsRuleServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl=DEBUG",
        "logging.level.it.gov.pagopa.reward.service.reward.evaluate.RuleEngineServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.reward.RewardCalculatorMediatorServiceImpl=WARN",
})
@Slf4j
abstract class BaseTransactionProcessorTest extends BaseIntegrationTest {

    @SpyBean
    protected RewardContextHolderService rewardContextHolderService;
    @Autowired
    protected HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    protected UserInitiativeCountersRepository userInitiativeCountersRepository;
    @Autowired
    protected TransactionProcessedRepository transactionProcessedRepository;

    @AfterEach
    void clearData(){
        transactionProcessedRepository.deleteAll().block();
    }

    protected void publishRewardRules(List<InitiativeReward2BuildDTO> initiatives) {
        int[] expectedRules = {0};
        initiatives.forEach(i -> {
            expectedRules[0] += RewardRuleConsumerConfigTest.calcDroolsRuleGenerated(i);
            publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, i);
        });

        RewardRuleConsumerConfigTest.waitForKieContainerBuild(expectedRules[0], rewardContextHolderService);
    }

    protected void onboardHpan(String hpan, LocalDateTime startInterval, LocalDateTime endInterval, String... initiativeIds){
        hpanInitiativesRepository.findById(hpan)
                .defaultIfEmpty(
                        HpanInitiatives.builder()
                                .hpan(hpan)
                                .onboardedInitiatives(new ArrayList<>())
                                .build())
                .map(hpan2initiative -> {
                    hpan2initiative.getOnboardedInitiatives().addAll(Arrays.stream(initiativeIds).map(initiativeId -> OnboardedInitiative.builder()
                                    .initiativeId(initiativeId)
                                    .activeTimeIntervals(List.of(ActiveTimeInterval.builder()
                                            .startInterval(startInterval)
                                            .endInterval(endInterval)
                                            .build()))
                                    .build())
                            .collect(Collectors.toList()));
                    return hpan2initiative;
                })
                .flatMap(hpanInitiativesRepository::save)
                .block();
    }

    protected void saveUserInitiativeCounter(TransactionDTO trx, InitiativeCounters initiativeRewardCounter, String initiativeIdExhausted) {
        userInitiativeCountersRepository.save(UserInitiativeCounters.builder()
                .userId(trx.getUserId())
                .initiatives(new HashMap<>(Map.of(
                        initiativeIdExhausted,
                        initiativeRewardCounter
                )))
                .build()).block();
    }

    protected void assertRewardedState(RewardTransactionDTO evaluation, String rewardedInitiativeId, BigDecimal expectedReward, boolean expectedCap) {
        Assertions.assertEquals(Collections.emptyList(), evaluation.getRejectionReasons());
        Assertions.assertEquals(Collections.emptyMap(), evaluation.getInitiativeRejectionReasons());
        Assertions.assertFalse(evaluation.getRewards().isEmpty());
        Assertions.assertEquals("REWARDED", evaluation.getStatus());

        final Reward initiativeReward = evaluation.getRewards().get(rewardedInitiativeId);
        Assertions.assertNotNull(initiativeReward);

        TestUtils.assertBigDecimalEquals(expectedReward, initiativeReward.getAccruedReward());
        if (!expectedCap) {
            TestUtils.assertBigDecimalEquals(initiativeReward.getProvidedReward(), initiativeReward.getAccruedReward());
        } else {
            Assertions.assertTrue(initiativeReward.getProvidedReward().compareTo(initiativeReward.getAccruedReward())>0);
        }
    }
}