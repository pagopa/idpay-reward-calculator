package it.gov.pagopa.reward.connector.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.reward.connector.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedService;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import it.gov.pagopa.reward.utils.Utils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Stubber;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.util.Pair;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@TestPropertySource(properties = {
        "app.trx-retries.counters-update.delayMillis:2",

        "logging.level.it.gov.pagopa.reward.service.reward.RewardNotifierServiceImpl=OFF",
        "logging.level.it.gov.pagopa.common.utils.MethodRetryUtils=OFF",
        "logging.level.it.gov.pagopa.common.kafka.service.ErrorNotifierServiceImpl=OFF",
})
class UncommittableErrorTest extends BaseTransactionProcessorTest {

    public static final String DUPLICATE_SUFFIX = "_DUPLICATE";

    public static final String BINDER_TRX_PROCESSOR_OUT_OUT_0 = "trxProcessorOut-out-0";
    public static final String BINDER_ERRORS_OUT_0 = "errors-out-0";

    @SpyBean
    private StreamBridge streamBridgeSpy;

    @Value("${app.trx-retries.counters-update.retries}")
    private int maxCountersUpdateRetries;
    @Value("${app.trx-retries.reward-notify.retries}")
    private int maxRewardNotifyRetries;

    @SpyBean
    private TransactionProcessedService transactionProcessedServiceSpy;

    private final int N = 1000;
    private final int duplicateTrx = Math.min(100, N);

    private int publishedIntoErrorTopicInstead=0;

    private final Map<String, AtomicInteger> trxId2InvocationCounts=new HashMap<>();

    @PostConstruct
    void init(){
        kafkaTestUtilitiesService.setupCommitLogMemoryAppender();
    }

    @Test
    void test() throws JsonProcessingException {
        long maxWaitingMs = 60000;

        publishRewardRules();

        List<String> trxs = buildValidPayloads(N);

        int totalSendMessages = trxs.size() + duplicateTrx;

        long timePublishOnboardingStart = System.currentTimeMillis();
        int[] i = new int[]{0};
        trxs.forEach(p -> {
            final String userId = Utils.readUserId(p);
            kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, userId, p);

            // to test duplicate trx and their right processing order
            if (i[0] < duplicateTrx) {
                i[0]++;
                kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, userId, p.replaceFirst("(senderCode\":\"[^\"]+)", "$1%s".formatted(DUPLICATE_SUFFIX)));
            }
        });
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = kafkaTestUtilitiesService.consumeMessages(topicRewardProcessorOutcome, N - publishedIntoErrorTopicInstead, maxWaitingMs);
        List<ConsumerRecord<String, String>> errorsConsumed = kafkaTestUtilitiesService.consumeMessages(topicErrors, publishedIntoErrorTopicInstead, maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(N - publishedIntoErrorTopicInstead, payloadConsumed.size());
        Assertions.assertEquals(publishedIntoErrorTopicInstead, errorsConsumed.size());
        Assertions.assertEquals(N, transactionProcessedRepository.count().block());

        Stream.concat(payloadConsumed.stream(), errorsConsumed.stream()).forEach(p -> {
            try {
                RewardTransactionDTO payload = objectMapper.readValue(p.value(), RewardTransactionDTO.class);
                checkResponse(payload);
                Assertions.assertEquals(payload.getUserId(), p.key());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        assertCounters();

        kafkaTestUtilitiesService.assertCommitOrder("REWARD", totalSendMessages);

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) trx messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                totalSendMessages,
                N,
                duplicateTrx,
                timePublishingOnboardingRequest,
                timeConsumerResponseEnd,
                timeEnd - timePublishOnboardingStart
        );

        checkOffsets(totalSendMessages, N - publishedIntoErrorTopicInstead);
    }

    private void assertCounters() throws JsonProcessingException {

        Assertions.assertEquals(
                objectMapper.writeValueAsString(
                        expectedCounters.values().stream()
                                .flatMap(c -> c.getInitiatives().values().stream())
                                .sorted(Comparator.comparing(UserInitiativeCounters::getUserId).thenComparing(UserInitiativeCounters::getInitiativeId))
                                .peek(counter -> counter.setUpdateDate(counter.getUpdateDate().truncatedTo(ChronoUnit.DAYS)))
                                .toList()
                ),
                objectMapper.writeValueAsString(Objects.requireNonNull(
                                userInitiativeCountersRepositorySpy.findAll().collectList().block()).stream()
                        .sorted(Comparator.comparing(UserInitiativeCounters::getUserId).thenComparing(UserInitiativeCounters::getInitiativeId))
                        .peek(counter -> counter.setUpdateDate(counter.getUpdateDate().truncatedTo(ChronoUnit.DAYS)))
                        .toList()
                ));
    }

    private List<String> buildValidPayloads(int bias) {
        return IntStream.range(0, bias)
                .mapToObj(this::mockInstance)
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    // region initiative build
    private static final String INITIATIVE_ID1 = "INITIATIVEID";
    private static final String INITIATIVE_ID2 = "INITIATIVEID2";
    public static final BigDecimal EXPECTED_REWARD = TestUtils.bigDecimalValue(0.5);

    private void publishRewardRules() {
        int[] expectedRules = {0};
        Stream.of(
                        buildInitiative(INITIATIVE_ID1),
                        buildInitiative(INITIATIVE_ID2)
                )
                .peek(i -> expectedRules[0] += RewardRuleConsumerConfigTest.calcDroolsRuleGenerated(i))
                .forEach(i -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, i));

        RewardRuleConsumerConfigTest.waitForKieContainerBuild(expectedRules[0], rewardContextHolderService);
    }

    private static InitiativeReward2BuildDTO buildInitiative(String initiativeId) {
        return InitiativeReward2BuildDTOFaker.mockInstanceBuilder(0, Collections.emptySet(), RewardValueDTO.class)
                .initiativeId(initiativeId)
                .initiativeName("NAME_" + initiativeId)
                .organizationId("ORGANIZATIONID_" + initiativeId)
                .trxRule(InitiativeTrxConditions.builder()
                        .threshold(ThresholdDTO.builder()
                                .from(BigDecimal.valueOf(5))
                                .fromIncluded(true)
                                .build())
                        .build())
                .rewardRule(RewardValueDTO.builder()
                        .rewardValue(BigDecimal.TEN)
                        .build())
                .build();
    }
    //endregion

    private TransactionDTO mockInstance(int bias) {
        int useCase = bias % useCases.size();
        final TransactionDTO trx = useCases.get(useCase).getFirst().apply(bias);
        onboardTrxHPanNoCreateUserCounter(trx, INITIATIVE_ID1, INITIATIVE_ID2);
        return trx;
    }

    private void checkResponse(RewardTransactionDTO rewardedTrx) {
        String hpan = rewardedTrx.getHpan();
        int biasRetrieve = Integer.parseInt(hpan.substring(4));
        try {
            useCases.get(biasRetrieve % useCases.size()).getSecond().accept(rewardedTrx);
            Assertions.assertFalse(rewardedTrx.getSenderCode().endsWith(DUPLICATE_SUFFIX), "Unexpected senderCode: " + rewardedTrx.getSenderCode());
            Assertions.assertEquals(CommonUtilities.centsToEuro(rewardedTrx.getAmountCents()), rewardedTrx.getAmount());
        } catch (Throwable e) {
            System.err.printf("UseCase %d (bias %d) failed: %n", biasRetrieve % useCases.size(), biasRetrieve);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (e.getCause() instanceof AssertionFailedError assertionFailedError) {
                throw assertionFailedError;
            }
            Assertions.fail(e);
        }
    }

    //region useCases
    private final List<Pair<Function<Integer, TransactionDTO>, Consumer<RewardTransactionDTO>>> useCases = List.of(
            // useCase 0: rewarded with no previous counter and no errors
            Pair.of(
                    this::buildTrx,
                    this::assertRewardedState
            ),

            // useCase 1: rewarded with previous counter 2 update and not counters and no errors
            Pair.of(
                    i -> {
                        TransactionDTO trx = buildTrx(i);
                        storeAsAlreadyProcessed(i);
                        return trx;
                    },
                    evaluation -> {
                        assertInitiative1RewardedStateWhenAlreadyProcessed(evaluation);
                        assertInitiative2RewardedStateWhenAlreadyProcessed(evaluation);
                    }
            ),

            // useCase 2: rewarded with previous counter 2 update and old counters stored and no errors
            Pair.of(
                    i -> {
                        TransactionDTO trx = buildTrx(i);
                        TransactionProcessed transactionProcessed = storeAsAlreadyProcessed(i);

                        RewardCounters rc1 = transactionProcessed.getRewards().get(INITIATIVE_ID1).getCounters();
                        saveUserInitiativeCounter(trx, UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID1)
                                .version(rc1.getVersion() - 1)
                                .trxNumber(1L)
                                .build());

                        RewardCounters rc2 = transactionProcessed.getRewards().get(INITIATIVE_ID2).getCounters();
                        UserInitiativeCounters c2 = saveUserInitiativeCounter(trx, UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID2)
                                .version(rc2.getVersion())
                                .trxNumber(2L)
                                .totalAmount(TestUtils.bigDecimalValue(10))
                                .totalReward(EXPECTED_REWARD)
                                .build());

                        UserInitiativeCountersWrapper userCounter = createUserCounter(trx);
                        userCounter.getInitiatives().put(INITIATIVE_ID2, c2);

                        return trx;
                    },
                    evaluation -> {
                        assertInitiative1RewardedStateWhenAlreadyProcessed(evaluation);
                        assertInitiative2RewardedStateWhenAlreadyProcessed(evaluation);
                    }
            ),

            // useCase 3: rewarded with errors storing just INITIATIVE_ID counter
            Pair.of(
                    i -> {
                        TransactionDTO trx = buildTrx(i);
                        configureErrorWhenStoringCounter( 1, trx, true);
                        return trx;
                    },
                    evaluation -> {
                        assertRewardedState(evaluation);
                        verifyMessageProcessingExecutionTimes(evaluation, 1);
                        Assertions.assertEquals(2, trxId2InvocationCounts.get(evaluation.getId()).get());
                    }
            ),

            // useCase 4: rewarded with errors storing both INITIATIVE_ID and INITIATIVE_ID2 counters
            Pair.of(
                    i -> {
                        TransactionDTO trx = buildTrx(i);
                        configureErrorWhenStoringCounter(1, trx, false);
                        return trx;
                    },
                    evaluation -> {
                        assertRewardedState(evaluation);
                        verifyMessageProcessingExecutionTimes(evaluation, 1);
                        Assertions.assertEquals(2, trxId2InvocationCounts.get(evaluation.getId()).get());
                    }
            ),

            // useCase 5: rewarded with errors storing both INITIATIVE_ID and INITIATIVE_ID2 counters exceeding max retry
            Pair.of(
                    i -> {
                        TransactionDTO trx = buildTrx(i);
                        configureErrorWhenStoringCounter(maxCountersUpdateRetries +1, trx, false);
                        return trx;
                    },
                    evaluation -> {
                        assertRewardedState(evaluation);
                        verifyMessageProcessingExecutionTimes(evaluation, 2);
                        Assertions.assertEquals(maxCountersUpdateRetries +2, trxId2InvocationCounts.get(evaluation.getId()).get());
                    }
            ),

            // useCase 6: cannot publish result, it will try to send into error topic, but at first try it will go into error also here
            Pair.of(
                    i -> {
                        TransactionDTO trx = buildTrx(i);
                        configureErrorWhenPublishingResults(1, trx, i);
                        return trx;
                    },
                    evaluation -> {
                        assertRewardedState(evaluation);
                        verifyMessageProcessingExecutionTimes(evaluation, 1);
                        verifyNotifyAttempts(2, evaluation);
                    }
            ),

            // useCase 7: cannot publish result, it will try to send into error topic, but at first try it will go into error also here exceeding max retry
            Pair.of(
                    i -> {
                        TransactionDTO trx = buildTrx(i);
                        configureErrorWhenPublishingResults(maxRewardNotifyRetries +1, trx, i);
                        return trx;
                    },
                    evaluation -> {
                        assertRewardedState(evaluation);
                        verifyMessageProcessingExecutionTimes(evaluation, 2);
                        verifyNotifyAttempts(maxRewardNotifyRetries + 2, evaluation);
                    }
            )
    );

    private void assertRewardedState(RewardTransactionDTO evaluation) {
        assertRewardedState(evaluation, INITIATIVE_ID1, false, 1L, 5, 0, false);
        assertInitiative2RewardedStateWhenAlreadyProcessed(evaluation);
    }

    private void assertInitiative1RewardedStateWhenAlreadyProcessed(RewardTransactionDTO evaluation) {
        assertRewardedState(evaluation, INITIATIVE_ID1, false, 2L, 10, 0.5, false);
    }

    private void assertInitiative2RewardedStateWhenAlreadyProcessed(RewardTransactionDTO evaluation) {
        assertRewardedState(evaluation, INITIATIVE_ID2, false, 1L, 5, 0, false);
    }

    private TransactionProcessed storeAsAlreadyProcessed(Integer bias) {
        RewardTransactionDTO rewarded = RewardTransactionDTOFaker.mockInstance(bias);
        rewarded.setStatus(RewardConstants.REWARD_STATE_REWARDED);
        rewarded.setElaborationDateTime(LocalDateTime.now());

        Reward r1 = new Reward(INITIATIVE_ID1, "ORGANIZATIONID_" + INITIATIVE_ID1, EXPECTED_REWARD);
        RewardCounters rc1 = RewardCounters.builder()
                .version(2L)
                .trxNumber(2L)
                .totalAmount(TestUtils.bigDecimalValue(10))
                .totalReward(EXPECTED_REWARD.multiply(TestUtils.bigDecimalValue(2)).setScale(2, RoundingMode.UNNECESSARY))
                .build();
        r1.setCounters(rc1
        );
        Reward r2 = new Reward(INITIATIVE_ID2, "ORGANIZATIONID_" + INITIATIVE_ID2, EXPECTED_REWARD);
        RewardCounters rc2 = RewardCounters.builder()
                .version(1L)
                .trxNumber(1L)
                .totalAmount(TestUtils.bigDecimalValue(5))
                .totalReward(EXPECTED_REWARD)
                .build();
        r2.setCounters(rc2
        );

        rewarded.setRewards(Map.of(
                INITIATIVE_ID1, r1,
                INITIATIVE_ID2, r2));

        UserInitiativeCountersWrapper userCounter = createUserCounter(rewarded);
        userCounter.getInitiatives().put(INITIATIVE_ID1, UserInitiativeCounters.builder(rewarded.getUserId(), INITIATIVE_ID1)
                .version(2)
                .trxNumber(2L)
                .totalReward(EXPECTED_REWARD.multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.UNNECESSARY))
                .totalAmount(TestUtils.bigDecimalValue(10))
                .build());

        return (TransactionProcessed) transactionProcessedServiceSpy.save(rewarded).block();
    }

    private TransactionDTO buildTrx(Integer i) {
        return onboardTrxHpanAndIncreaseCounters(
                TransactionDTOFaker.mockInstanceBuilder(i)
                        .amount(BigDecimal.valueOf(5_00))
                        .build(),
                INITIATIVE_ID1, INITIATIVE_ID2);
    }

    private void assertRewardedState(RewardTransactionDTO evaluation, String rewardedInitiativeId, boolean expectedCap, long expectedCounterTrxNumber, double expectedCounterTotalAmount, double preCurrentTrxCounterTotalReward, boolean expectedCounterBudgetExhausted) {
        assertRewardedState(evaluation, 2, rewardedInitiativeId, EXPECTED_REWARD, expectedCap, expectedCounterTrxNumber, expectedCounterTotalAmount, EXPECTED_REWARD.doubleValue() + preCurrentTrxCounterTotalReward, expectedCounterBudgetExhausted, false, false);
    }

    private TransactionDTO onboardTrxHpanAndIncreaseCounters(TransactionDTO trx, String... initiativeIds) {
        UserInitiativeCountersWrapper userInitiativeCountersWrapper = onboardTrxHPan(trx, initiativeIds);

        Arrays.stream(initiativeIds).forEach(id -> {
            InitiativeConfig initiativeConfig = Objects.requireNonNull(droolsRuleRepository.findById(id).block()).getInitiativeConfig();
            updateInitiativeCounters(userInitiativeCountersWrapper
                            .getInitiatives().computeIfAbsent(id, x -> UserInitiativeCounters.builder(trx.getUserId(), id).build()),
                    trx, EXPECTED_REWARD, initiativeConfig);
        });

        return trx;
    }

    private void configureErrorWhenStoringCounter(int attemptsBeforeOk, TransactionDTO trx, boolean storeFirst) {
        AtomicInteger storedAttempts = new AtomicInteger();
        trxId2InvocationCounts.put(trx.getId(), storedAttempts);
        Mockito
                .doAnswer(a -> {
                    //noinspection unchecked
                    Iterable<UserInitiativeCounters> storingCounters = a.getArgument(0, Iterable.class);

                    return Flux.<UserInitiativeCounters>defer(
                            () -> {
                                if (storedAttempts.incrementAndGet() > attemptsBeforeOk) {
                                    return Flux.fromIterable(storingCounters)
                                            .flatMap(userInitiativeCountersRepositorySpy::save);
                                } else {
                                    if (storeFirst) {
                                        return userInitiativeCountersRepositorySpy.save(storingCounters.iterator().next())
                                                .thenMany(Flux.error(new RuntimeException("DUMMYEXCEPTIONSTORINGSECONDCOUNTER")));
                                    } else {
                                        return Flux.error(new RuntimeException("DUMMYEXCEPTIONSTORINGALLCOUNTERS"));
                                    }
                                }
                            });
                })
                .when(userInitiativeCountersRepositorySpy)
                .saveAll(configureMockitoArgListCounters(trx));
    }

    private void configureErrorWhenPublishingResults(int attemptsBeforeOk, TransactionDTO trx, int bias) {
        AtomicInteger publishedAttempts = new AtomicInteger(0);
        boolean throwException = bias < N / 2;

        Stubber rewardNotifyStub;
        if(throwException){
            rewardNotifyStub = Mockito.doThrow(new RuntimeException("DUMMYEXCEPTIONPUBLISHINGRESULTS"));
        } else {
            rewardNotifyStub = Mockito.doReturn(false);
        }
        rewardNotifyStub
                        .when(streamBridgeSpy)
                                .send(Mockito.eq(BINDER_TRX_PROCESSOR_OUT_OUT_0), configureMockitoArgMessageTrx(trx));

        publishedIntoErrorTopicInstead++;

        Mockito
                .doAnswer(a -> {
                    if (publishedAttempts.getAndIncrement() >= attemptsBeforeOk) {
                        return a.callRealMethod();
                    } else {
                        if(throwException){
                            throw new RuntimeException("DUMMYEXCEPTIONPUBLISHINGRESULTSINTOERRORTOPIC");
                        } else {
                            return false;
                        }
                    }
                })
                .when(streamBridgeSpy)
                .send(Mockito.eq(BINDER_ERRORS_OUT_0), configureMockitoArgMessageTrx(trx));
    }

    private void verifyMessageProcessingExecutionTimes(RewardTransactionDTO evaluation, int executionTimes) {
        Mockito.verify(transactionProcessedServiceSpy, Mockito.times(executionTimes)).checkDuplicateTransactions(configureMockitoArgTransaction(evaluation));
        Mockito.verify(transactionProcessedServiceSpy).save(configureMockitoArgRewardTransaction(evaluation));
    }

    private void verifyNotifyAttempts(int maxRewardNotifyAttempts, RewardTransactionDTO evaluation) {
        Mockito.verify(streamBridgeSpy, Mockito.times(maxRewardNotifyAttempts))
                .send(Mockito.eq(BINDER_TRX_PROCESSOR_OUT_OUT_0), configureMockitoArgMessageTrx(evaluation));
        Mockito.verify(streamBridgeSpy, Mockito.times(maxRewardNotifyAttempts))
                .send(Mockito.eq(BINDER_ERRORS_OUT_0), configureMockitoArgMessageTrx(evaluation));
    }

    private static TransactionDTO configureMockitoArgTransaction(TransactionDTO trx) {
        return Mockito.argThat(t -> t.getUserId().equals(trx.getUserId()) && !t.getSenderCode().endsWith(DUPLICATE_SUFFIX));
    }

    private static RewardTransactionDTO configureMockitoArgRewardTransaction(TransactionDTO trx) {
        return Mockito.argThat(t -> t.getUserId().equals(trx.getUserId()) && !t.getSenderCode().endsWith(DUPLICATE_SUFFIX));
    }

    private static Iterable<UserInitiativeCounters> configureMockitoArgListCounters(TransactionDTO trx) {
        return Mockito.argThat(ctrs -> ctrs.iterator().next().getUserId().equals(trx.getUserId()));
    }

    private static Message<RewardTransactionDTO> configureMockitoArgMessageTrx(TransactionDTO trx) {
        return Mockito.argThat(r -> r.getPayload().getUserId().equals(trx.getUserId()) && !r.getPayload().getSenderCode().endsWith(DUPLICATE_SUFFIX));
    }
    //endregion

}