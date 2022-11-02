package it.gov.pagopa.reward.event.processor;

import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
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
import it.gov.pagopa.reward.service.LockServiceImpl;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Semaphore;

@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.reward.service.build.RewardRule2DroolsRuleServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl=DEBUG",
        "logging.level.it.gov.pagopa.reward.service.reward.evaluate.RuleEngineServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.reward.RewardCalculatorMediatorServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.reward.trx.TransactionProcessedServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.BaseKafkaConsumer=WARN",
})
@Slf4j
abstract class BaseTransactionProcessorTest extends BaseIntegrationTest {

    protected final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected final DateTimeFormatter weekFormatter = DateTimeFormatter.ofPattern("yyyy-MM-W", Locale.ITALY);
    protected final DateTimeFormatter monthlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    protected final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");

    @SpyBean
    protected RewardContextHolderService rewardContextHolderService;
    @Autowired
    protected HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    protected UserInitiativeCountersRepository userInitiativeCountersRepository;
    @Autowired
    protected TransactionProcessedRepository transactionProcessedRepository;

    @Autowired
    protected LockServiceImpl lockService;

    @AfterEach
    void checkLockBouquet() throws NoSuchFieldException, IllegalAccessException {
        final Field locksField = LockServiceImpl.class.getDeclaredField("locks");
        locksField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Integer, Semaphore> locks = (Map<Integer, Semaphore>)locksField.get(lockService);
        locks.values().forEach(l->Assertions.assertEquals(1, l.availablePermits()));
    }

    @AfterEach
    void clearData(){
        transactionProcessedRepository.deleteAll().block();
        userInitiativeCountersRepository.deleteAll().block();
        hpanInitiativesRepository.deleteAll().block();
        droolsRuleRepository.deleteAll().block();
    }

    protected void publishRewardRules(List<InitiativeReward2BuildDTO> initiatives) {
        int[] expectedRules = {0};
        initiatives.forEach(i -> {
            expectedRules[0] += RewardRuleConsumerConfigTest.calcDroolsRuleGenerated(i);
            publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, i);
        });

        RewardRuleConsumerConfigTest.waitForKieContainerBuild(expectedRules[0], rewardContextHolderService);
        initiatives.forEach(i-> Assertions.assertNotNull(rewardContextHolderService.getInitiativeConfig(i.getInitiativeId())));
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
                            .toList());
                    return hpan2initiative;
                })
                .flatMap(hpanInitiativesRepository::save)
                .block();
    }

    protected void saveUserInitiativeCounter(TransactionDTO trx, InitiativeCounters initiativeRewardCounter) {
        userInitiativeCountersRepository.save(UserInitiativeCounters.builder()
                .userId(trx.getUserId())
                .initiatives(new HashMap<>(Map.of(
                        initiativeRewardCounter.getInitiativeId(),
                        initiativeRewardCounter
                )))
                .build()).block();
    }

    protected void assertRewardedState(RewardTransactionDTO evaluation, String rewardedInitiativeId, BigDecimal expectedReward, boolean expectedCap, long expectedCounterTrxNumber, double expectedCounterTotalAmount, double expectedCounterTotalReward, boolean expectedCounterBudgetExhausted) {
        assertRewardedState(evaluation, 1, rewardedInitiativeId, expectedReward, expectedCap, expectedCounterTrxNumber, expectedCounterTotalAmount, expectedCounterTotalReward, expectedCounterBudgetExhausted, false, false);
    }
    protected void assertRewardedState(RewardTransactionDTO evaluation, int expectedInitiativeRewarded, String rewardedInitiativeId, BigDecimal expectedReward, boolean expectedCap, long expectedCounterTrxNumber, double expectedCounterTotalAmount, double expectedCounterTotalReward, boolean expectedCounterBudgetExhausted, boolean isRefund, boolean isCompleteRefund) {
        Assertions.assertEquals(Collections.emptyList(), evaluation.getRejectionReasons());
        Assertions.assertEquals(Collections.emptyMap(), evaluation.getInitiativeRejectionReasons());
        Assertions.assertFalse(evaluation.getRewards().isEmpty());
        Assertions.assertEquals("REWARDED", evaluation.getStatus());

        final Reward initiativeReward = evaluation.getRewards().get(rewardedInitiativeId);
        Assertions.assertNotNull(initiativeReward);

        Assertions.assertEquals(rewardedInitiativeId, initiativeReward.getInitiativeId());
        Assertions.assertEquals("ORGANIZATIONID_" + rewardedInitiativeId, initiativeReward.getOrganizationId());
        TestUtils.assertBigDecimalEquals(expectedReward, initiativeReward.getAccruedReward());
        if (!expectedCap) {
            TestUtils.assertBigDecimalEquals(initiativeReward.getProvidedReward(), initiativeReward.getAccruedReward());
        } else {
            Assertions.assertTrue(initiativeReward.getProvidedReward().compareTo(initiativeReward.getAccruedReward())>0);
        }

        Assertions.assertEquals(expectedCounterTrxNumber, initiativeReward.getCounters().getTrxNumber());
        Assertions.assertEquals(TestUtils.bigDecimalValue(expectedCounterTotalAmount), initiativeReward.getCounters().getTotalAmount());
        Assertions.assertEquals(TestUtils.bigDecimalValue(expectedCounterTotalReward), initiativeReward.getCounters().getTotalReward());
        Assertions.assertEquals(expectedCounterBudgetExhausted, initiativeReward.getCounters().isExhaustedBudget());
        Assertions.assertEquals(isRefund, initiativeReward.isRefund());
        Assertions.assertEquals(isCompleteRefund, initiativeReward.isCompleteRefund());

        Assertions.assertEquals(expectedInitiativeRewarded, evaluation.getRewards().size());
    }

    protected void checkOffsets(long expectedReadMessages, long exptectedPublishedResults){
        long timeStart = System.currentTimeMillis();
        final Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicRewardProcessorRequest, groupIdRewardProcessorRequest,expectedReadMessages, 20, 1000);
        long timeCommitChecked = System.currentTimeMillis();
        final Map<TopicPartition, Long> destPublishedOffsets = checkPublishedOffsets(topicRewardProcessorOutcome, exptectedPublishedResults);
        long timePublishChecked = System.currentTimeMillis();

        System.out.printf("""
                        ************************
                        Time occurred to check committed offset: %d millis
                        Time occurred to check published offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        Dest Topic Published Offsets: %s
                        ************************
                        """,
                timeCommitChecked - timeStart,
                timePublishChecked - timeCommitChecked,
                srcCommitOffsets,
                destPublishedOffsets
        );
    }
}