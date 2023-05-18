package it.gov.pagopa.reward.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.RewardNotifierService;
import it.gov.pagopa.reward.service.reward.evaluate.RuleEngineService;
import it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateService;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedService;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import it.gov.pagopa.reward.utils.Utils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class UncommittabeErrorTest extends BaseTransactionProcessorTest {

    public static final String DUPLICATE_SUFFIX = "_DUPLICATE";

    @Autowired
    private TransactionProcessedService transactionProcessedService;
    @Autowired
    private Transaction2RewardTransactionMapper transaction2RewardTransactionMapper;

    @SpyBean
    private RuleEngineService ruleEngineServiceSpy;
    @SpyBean
    private UserInitiativeCountersUpdateService userInitiativeCountersUpdateServiceSpy;
    @SpyBean
    private RewardNotifierService rewardNotifierServiceSpy;

    @Test
    void test() throws JsonProcessingException {
        int trx = 1000; // use even number
        int duplicateTrx = Math.min(100, trx);
        long maxWaitingMs = 60000;

        publishRewardRules();

        List<String> trxs = buildValidPayloads(0, trx);

        long totalSendMessages = trxs.size() + duplicateTrx;

        long timePublishOnboardingStart = System.currentTimeMillis();
        int[] i = new int[]{0};
        trxs.forEach(p -> {
            final String userId = Utils.readUserId(p);
            publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, userId, p);

            // to test duplicate trx and their right processing order
            if (i[0] < duplicateTrx) {
                i[0]++;
                publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, userId, p.replaceFirst("(senderCode\":\"[^\"]+)", "$1%s".formatted(DUPLICATE_SUFFIX)));
            }
        });
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = consumeMessages(topicRewardProcessorOutcome, trx, maxWaitingMs); // TODO probably more than trx
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(trx, payloadConsumed.size());
        Assertions.assertEquals(trx, transactionProcessedRepository.count().block());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            RewardTransactionDTO payload = objectMapper.readValue(p.value(), RewardTransactionDTO.class);
            checkResponse(payload);
            Assertions.assertEquals(payload.getUserId(), p.key());
        }

        assertCounters();

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) trx messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                totalSendMessages,
                trx,
                duplicateTrx,
                timePublishingOnboardingRequest,
                timeConsumerResponseEnd,
                timeEnd - timePublishOnboardingStart
        );

        // TODO too expensive?
        checkOffsets(totalSendMessages, trx); // +1 due to other applicationName useCase
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
                                userInitiativeCountersRepository.findAll().collectList().block()).stream()
                        .sorted(Comparator.comparing(UserInitiativeCounters::getUserId).thenComparing(UserInitiativeCounters::getInitiativeId))
                        .peek(counter -> counter.setUpdateDate(counter.getUpdateDate().truncatedTo(ChronoUnit.DAYS)))
                        .toList()
                ));
    }

    private List<String> buildValidPayloads(int bias, int validOnboardings) {
        return IntStream.range(bias, bias + validOnboardings)
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
                .forEach(i -> publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, i));

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
            Assertions.assertEquals(Utils.centsToEuro(rewardedTrx.getAmountCents()), rewardedTrx.getAmount());
        } catch (Exception e) {
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
    private final LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(0, 0));

    private final List<Pair<Function<Integer, TransactionDTO>, Consumer<RewardTransactionDTO>>> useCases = List.of(
            // useCase 0: rewarded with no previous counter and no errors
            Pair.of(
                    this::buildTrx,
                    evaluation -> {
                        assertRewardedState(evaluation, INITIATIVE_ID1, false, 1L, 5, 0, false);
                        assertInitiative2RewardedStateWhenAlreadyProcessed(evaluation);
                    }
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
            )

            // useCase 3: rewarded with errors storing just INITIATIVE_ID TODO

            // useCase 4: rewarded with errors storing just INITIATIVE_ID2 TODO

            // useCase 5: rewarded with errors storing both INITIATIVE_ID and INITIATIVE_ID2 TODO

            // useCase 6: cannot publish result neither in error topic TODO

            // useCase 7: cannot publish result neither in error topic when recovering TODO
    );

    private void assertInitiative2RewardedStateWhenAlreadyProcessed(RewardTransactionDTO evaluation) {
        assertRewardedState(evaluation, INITIATIVE_ID2, false, 1L, 5, 0, false);
    }

    private void assertInitiative1RewardedStateWhenAlreadyProcessed(RewardTransactionDTO evaluation) {
        assertRewardedState(evaluation, INITIATIVE_ID1, false, 2L, 10, 0.5, false);
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
                .totalReward(EXPECTED_REWARD.multiply(BigDecimal.valueOf(2)).setScale(2,RoundingMode.UNNECESSARY))
                .totalAmount(TestUtils.bigDecimalValue(10))
                .build());

        return (TransactionProcessed) transactionProcessedService.save(rewarded).block();
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
    //endregion

}