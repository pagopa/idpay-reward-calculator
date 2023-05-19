package it.gov.pagopa.reward.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.common.reactive.LockServiceImpl;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
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
        "logging.level.it.gov.pagopa.reward.service.reward.trx.RecoveryProcessedTransactionServiceImpl=WARN",
        "logging.level.it.gov.pagopa.common.kafka.consumer.BaseKafkaConsumer=WARN",
        "logging.level.it.gov.pagopa.common.kafka.consumer.BaseKafkaBlockingPartitionConsumer=WARN",
        "logging.level.it.gov.pagopa.common.reactive.PerformanceLogger=WARN",
        "logging.level.AUDIT=WARN",
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
    @SpyBean
    protected UserInitiativeCountersRepository userInitiativeCountersRepositorySpy;
    @Autowired
    protected TransactionProcessedRepository transactionProcessedRepository;
    @Autowired
    protected ReactiveMongoTemplate mongoTemplate;

    @Autowired
    protected LockServiceImpl lockService;

    protected final Map<String, UserInitiativeCountersWrapper> expectedCounters = new HashMap<>();

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
        userInitiativeCountersRepositorySpy.deleteAll().block();
        hpanInitiativesRepository.deleteAll().block();
        droolsRuleRepository.deleteAll().block();
    }

    @KafkaListener(topics = "${spring.cloud.stream.bindings.trxProcessorOut-out-0.destination}", groupId = "BaseTransactionProcessorTest")
    protected void transactionOutcomeListener(ConsumerRecord<String, byte[]> trxMessage) throws JsonProcessingException {
        RewardTransactionDTO trx = objectMapper.readValue(new String(trxMessage.value(), StandardCharsets.UTF_8), RewardTransactionDTO.class);
        mongoTemplate.save(new HashMap<>(Map.of("_id", trx.getId())), "transaction").block();
    }

    protected void publishRewardRules(List<InitiativeReward2BuildDTO> initiatives) {
        int[] expectedRules = {0};
        initiatives.forEach(i -> {
            expectedRules[0] += RewardRuleConsumerConfigTest.calcDroolsRuleGenerated(i);
            publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, i);
        });

        RewardRuleConsumerConfigTest.waitForKieContainerBuild(expectedRules[0], rewardContextHolderService);
        initiatives.forEach(i-> Assertions.assertNotNull(rewardContextHolderService.getInitiativeConfig(i.getInitiativeId()).block()));
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

    protected UserInitiativeCountersWrapper onboardTrxHPan(TransactionDTO trx, String... initiativeIds) {
        onboardTrxHPanNoCreateUserCounter(trx, initiativeIds);

        return createUserCounter(trx);
    }

    protected void onboardTrxHPanNoCreateUserCounter(TransactionDTO trx, String... initiativeIds) {
        onboardHpan(trx.getHpan(), trx.getTrxDate().toLocalDateTime(), trx.getTrxDate().toLocalDateTime().plusSeconds(1), initiativeIds);
    }

    protected UserInitiativeCountersWrapper createUserCounter(TransactionDTO trx) {
        return expectedCounters.computeIfAbsent(trx.getUserId(), u -> new UserInitiativeCountersWrapper(u, new LinkedHashMap<>()));
    }

    protected void updateInitiativeCounters(UserInitiativeCounters counters, TransactionDTO trx, BigDecimal expectedReward, InitiativeConfig initiativeConfig) {
        counters.setVersion(counters.getVersion()+1);
        updateCounters(counters, trx, expectedReward);
        if (initiativeConfig.isDailyThreshold()) {
            updateCounters(
                    counters.getDailyCounters().computeIfAbsent(trx.getTrxDate().format(dayFormatter), d -> new Counters()),
                    trx, expectedReward);
        }
        if (initiativeConfig.isWeeklyThreshold()) {
            updateCounters(
                    counters.getWeeklyCounters().computeIfAbsent(trx.getTrxDate().format(weekFormatter), d -> new Counters()),
                    trx, expectedReward);
        }
        if (initiativeConfig.isMonthlyThreshold()) {
            updateCounters(
                    counters.getMonthlyCounters().computeIfAbsent(trx.getTrxDate().format(monthlyFormatter), d -> new Counters()),
                    trx, expectedReward);
        }
        if (initiativeConfig.isYearlyThreshold()) {
            updateCounters(
                    counters.getYearlyCounters().computeIfAbsent(trx.getTrxDate().format(yearFormatter), d -> new Counters()),
                    trx, expectedReward);
        }
    }

    protected void updateCounters(Counters counters, TransactionDTO trx, BigDecimal expectedReward) {
        BigDecimal amountEuro;
        if(trx.getAmountCents()!=null){
            amountEuro= CommonUtilities.centsToEuro(trx.getAmountCents());
        } else {
            amountEuro=CommonUtilities.centsToEuro(trx.getAmount().longValue());
        }
        counters.setTrxNumber(counters.getTrxNumber() + 1);
        counters.setTotalAmount(counters.getTotalAmount().add(amountEuro));
        counters.setTotalReward(counters.getTotalReward().add(expectedReward).setScale(2, RoundingMode.UNNECESSARY));
    }

    protected UserInitiativeCounters saveUserInitiativeCounter(TransactionDTO trx, UserInitiativeCounters initiativeRewardCounter) {
        return userInitiativeCountersRepositorySpy.save(
                initiativeRewardCounter.toBuilder()
                        .userId(trx.getUserId())
                        .id(UserInitiativeCounters.buildId(trx.getUserId(), initiativeRewardCounter.getInitiativeId()))
                        .build())
                .block();
    }

    /** To assert rewarded charge transactions */
    protected void assertRewardedState(RewardTransactionDTO evaluation, String rewardedInitiativeId, BigDecimal expectedReward, boolean expectedCap, long expectedCounterTrxNumber, double expectedCounterTotalAmount, double expectedCounterTotalReward, boolean expectedCounterBudgetExhausted) {
        assertRewardedState(evaluation, 1, rewardedInitiativeId, expectedReward, expectedCap, expectedCounterTrxNumber, expectedCounterTotalAmount, expectedCounterTotalReward, expectedCounterBudgetExhausted, false, false);
    }
    /** To assert rewarded refunds (also negative is a refund) */
    protected void assertRewardedState(RewardTransactionDTO evaluation, int expectedInitiativeRewarded, String rewardedInitiativeId, BigDecimal expectedReward, boolean expectedDifferentAccrued, long expectedCounterTrxNumber, double expectedCounterTotalAmount, double expectedCounterTotalReward, boolean expectedCounterBudgetExhausted, boolean isRefund, boolean isCompleteRefund) {
        Assertions.assertEquals(RewardConstants.TRX_CHANNEL_RTD, evaluation.getChannel());
        Assertions.assertEquals(Collections.emptyList(), evaluation.getRejectionReasons());
        Assertions.assertEquals(Collections.emptyMap(), evaluation.getInitiativeRejectionReasons());
        Assertions.assertFalse(evaluation.getRewards().isEmpty());
        Assertions.assertEquals("REWARDED", evaluation.getStatus());

        final Reward initiativeReward = evaluation.getRewards().get(rewardedInitiativeId);
        Assertions.assertNotNull(initiativeReward);

        Assertions.assertEquals(rewardedInitiativeId, initiativeReward.getInitiativeId());
        Assertions.assertEquals("ORGANIZATIONID_" + rewardedInitiativeId, initiativeReward.getOrganizationId());
        TestUtils.assertBigDecimalEquals(expectedReward, initiativeReward.getAccruedReward());
        if (!expectedDifferentAccrued) {
            TestUtils.assertBigDecimalEquals(initiativeReward.getProvidedReward(), initiativeReward.getAccruedReward());
        } else {
            Assertions.assertTrue(initiativeReward.getProvidedReward().compareTo(initiativeReward.getAccruedReward())>0);
        }

        checkInitiativeRewardCounters(expectedCounterTrxNumber, expectedCounterTotalAmount, expectedCounterTotalReward, expectedCounterBudgetExhausted, isRefund, isCompleteRefund, initiativeReward);

        Assertions.assertEquals(expectedInitiativeRewarded, evaluation.getRewards().size());
    }

    private static void checkInitiativeRewardCounters(long expectedCounterTrxNumber, double expectedCounterTotalAmount, double expectedCounterTotalReward, boolean expectedCounterBudgetExhausted, boolean isRefund, boolean isCompleteRefund, Reward initiativeReward) {
        Assertions.assertEquals(expectedCounterTrxNumber, initiativeReward.getCounters().getTrxNumber());
        Assertions.assertEquals(TestUtils.bigDecimalValue(expectedCounterTotalAmount), initiativeReward.getCounters().getTotalAmount());
        Assertions.assertEquals(TestUtils.bigDecimalValue(expectedCounterTotalReward), initiativeReward.getCounters().getTotalReward());
        Assertions.assertEquals(expectedCounterBudgetExhausted, initiativeReward.getCounters().isExhaustedBudget());
        Assertions.assertEquals(isRefund, initiativeReward.isRefund());
        Assertions.assertEquals(isCompleteRefund, initiativeReward.isCompleteRefund());
    }

    /** Valid trx, processed by the ruleEngine, but which has been discarded by the initiative configuration */
    protected void assertRejectedState(RewardTransactionDTO evaluation, String initiativeId, List<String> expectedRejectionReasons, long expectedCounterTrxNumber, double expectedCounterTotalAmount, double expectedCounterTotalReward, boolean expectedCounterBudgetExhausted, boolean isRefund, boolean isCompleteRefund) {
        Assertions.assertEquals(Collections.emptyList(), evaluation.getRejectionReasons());
        boolean expectedZeroReward = expectedRejectionReasons == null;

        Assertions.assertEquals(expectedZeroReward
                ? Collections.emptyMap()
                : Map.of(initiativeId, expectedRejectionReasons),
                evaluation.getInitiativeRejectionReasons());
        Assertions.assertEquals(expectedZeroReward? "REWARDED" : "REJECTED", evaluation.getStatus());

        final Reward initiativeReward = evaluation.getRewards().get(initiativeId);
        if(expectedZeroReward){
            Assertions.assertNotNull(initiativeReward);
        }
        if(initiativeReward!=null) {
            TestUtils.assertBigDecimalEquals(BigDecimal.ZERO, initiativeReward.getAccruedReward());
            Assertions.assertEquals("ORGANIZATIONID_" + initiativeId, initiativeReward.getOrganizationId());

            checkInitiativeRewardCounters(expectedCounterTrxNumber, expectedCounterTotalAmount, expectedCounterTotalReward, expectedCounterBudgetExhausted, isRefund, isCompleteRefund, initiativeReward);
        }
    }

    /** assert on rejected trx (which has skipped the ruleEngine) */
    protected void assertRejectedTrx(RewardTransactionDTO evaluation, List<String> expectedRejectionReasons) {
        Assertions.assertEquals(expectedRejectionReasons, evaluation.getRejectionReasons());

        Assertions.assertEquals(Collections.emptyMap(), evaluation.getInitiativeRejectionReasons());
        Assertions.assertEquals("REJECTED", evaluation.getStatus());
        Assertions.assertEquals(Collections.emptyMap(), evaluation.getRewards());
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