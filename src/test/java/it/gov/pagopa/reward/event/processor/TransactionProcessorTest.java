package it.gov.pagopa.reward.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.mapper.Transaction2TransactionProcessedMapper;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.*;
import it.gov.pagopa.reward.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.repository.TransactionProcessedRepository;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.service.reward.RuleEngineService;
import it.gov.pagopa.reward.service.reward.TransactionProcessedService;
import it.gov.pagopa.reward.service.reward.UserInitiativeCountersUpdateService;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.rule.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "logging.level.it.gov.pagopa.reward.service.build.RewardRule2DroolsRuleServiceImpl=WARN",
        "logging.level.it.gov.pagopa.reward.service.build.KieContainerBuilderServiceImpl=DEBUG",
        "logging.level.it.gov.pagopa.reward.service.reward.RuleEngineServiceImpl=WARN",
})
@Slf4j
class TransactionProcessorTest extends BaseIntegrationTest {

    public static final long TRX_NUMBER_MIN_NUMBER_INITIATIVE_ID_TRXCOUNT = 9L;

    @SpyBean
    private RewardContextHolderService rewardContextHolderService;
    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    private UserInitiativeCountersRepository userInitiativeCountersRepository;
    @Autowired
    private TransactionProcessedRepository transactionProcessedRepository;
    @Autowired
    private TransactionProcessedService transactionProcessedService;
    @Autowired
    private Transaction2RewardTransactionMapper transaction2RewardTransactionMapper;

    @SpyBean
    private RuleEngineService ruleEngineServiceSpy;
    @SpyBean
    private UserInitiativeCountersUpdateService userInitiativeCountersUpdateServiceSpy;

    @Test
    void testTransactionProcessor() throws JsonProcessingException {
        int validTrx = 1000; // use even number
        int notValidTrx = errorUseCases.size();
        long maxWaitingMs = 30000;

        publishRewardRules();

        TransactionDTO trxDuplicated = TransactionDTOFaker.mockInstance(1);
        trxDuplicated.setIdTrxAcquirer("ALREADY_PROCESSED_TRX");
        transactionProcessedService.save(transaction2RewardTransactionMapper.apply(trxDuplicated)).block();

        List<String> trxs = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validTrx / 2));
        trxs.addAll(IntStream.range(0, notValidTrx).mapToObj(i -> errorUseCases.get(i).getFirst().get()).collect(Collectors.toList()));
        trxs.addAll(buildValidPayloads(errorUseCases.size() + (validTrx / 2) + notValidTrx, validTrx / 2));
        trxs.add(objectMapper.writeValueAsString(trxDuplicated));

        long timePublishOnboardingStart = System.currentTimeMillis();
        trxs.forEach(i -> {
            publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, readUserId(i), i);
            /* TODO fix parallel execution
             * publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, readUserId(i), i);
             */
        });
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = consumeMessages(topicRewardProcessorOutcome, validTrx, maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(validTrx, payloadConsumed.size());
        Assertions.assertEquals(validTrx+1, transactionProcessedRepository.count().block());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            checkResponse(objectMapper.readValue(p.value(), RewardTransactionDTO.class));
        }

        Assertions.assertEquals(
                objectMapper.writeValueAsString(expectedCounters.values().stream()
                        .sorted(Comparator.comparing(UserInitiativeCounters::getUserId))
                        .toList()),
                objectMapper.writeValueAsString(Objects.requireNonNull(userInitiativeCountersRepository.findAll().collectList().block()).stream()
                        .sorted(Comparator.comparing(UserInitiativeCounters::getUserId))
                        .toList()));

        checkErrorsPublished(notValidTrx, maxWaitingMs, errorUseCases);

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d) trx messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                validTrx + notValidTrx,
                validTrx,
                notValidTrx,
                timePublishingOnboardingRequest,
                timeConsumerResponseEnd,
                timeEnd - timePublishOnboardingStart
        );
    }

    private final Pattern userIdPatternMatch = Pattern.compile("\"userId\":\"([^\"]*)\"");
    private String readUserId(String payload) {
        final Matcher matcher = userIdPatternMatch.matcher(payload);
        return matcher.find() ? matcher.group(1) : "";
    }

    private List<String> buildValidPayloads(int bias, int validOnboardings) {
        return IntStream.range(bias, bias + validOnboardings)
                .mapToObj(this::mockInstance)
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    // region initiative build
    private static final String INITIATIVE_ID_THRESHOLD_BASED = "ID_0_THRESHOLD";
    private static final String INITIATIVE_ID_DAYOFWEEK_BASED = "ID_1_DAYOFWEEK";
    private static final String INITIATIVE_ID_MCC_BASED = "ID_2_MCC";
    private static final String INITIATIVE_ID_TRXCOUNT_BASED = "ID_3_TRXCOUNT";
    private static final String INITIATIVE_ID_REWARDLIMITS_BASED = "ID_4_REWARDLIMITS";
    private static final String INITIATIVE_ID_EXHAUSTED = "ID_5_EXHAUSTED";
    private static final String INITIATIVE_ID_EXHAUSTING = "ID_6_EXHAUSTING";

    private void publishRewardRules() {
        int[] expectedRules = {0};
        Stream.of(
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(0, Collections.emptySet(), RewardValueDTO.class)
                                .initiativeId(INITIATIVE_ID_THRESHOLD_BASED)
                                .trxRule(InitiativeTrxConditions.builder()
                                        .threshold(ThresholdDTO.builder()
                                                .from(BigDecimal.valueOf(5))
                                                .fromIncluded(true)
                                                .build())
                                        .build())
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(1, Collections.emptySet(), RewardValueDTO.class)
                                .initiativeId(INITIATIVE_ID_DAYOFWEEK_BASED)
                                .trxRule(InitiativeTrxConditions.builder()
                                        .daysOfWeek(new DayOfWeekDTO(List.of(
                                                DayOfWeekDTO.DayConfig.builder()
                                                        .daysOfWeek(Set.of(trxDate.getDayOfWeek()))
                                                        .build()
                                        )))
                                        .build())
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(2, Collections.emptySet(), RewardValueDTO.class)
                                .initiativeId(INITIATIVE_ID_MCC_BASED)
                                .trxRule(InitiativeTrxConditions.builder()
                                        .mccFilter(MccFilterDTO.builder()
                                                .allowedList(true)
                                                .values(Set.of("ACCEPTEDMCC"))
                                                .build())
                                        .build())
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(4, Collections.emptySet(), RewardValueDTO.class)
                                .trxRule(InitiativeTrxConditions.builder()
                                        .trxCount(TrxCountDTO.builder()
                                                .from(10L)
                                                .fromIncluded(true)
                                                .build())
                                        .build())
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .initiativeId(INITIATIVE_ID_TRXCOUNT_BASED)
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(3, Collections.emptySet(), RewardValueDTO.class)
                                .initiativeId(INITIATIVE_ID_REWARDLIMITS_BASED)
                                .trxRule(InitiativeTrxConditions.builder()
                                        .rewardLimits(List.of(
                                                RewardLimitsDTO.builder()
                                                        .frequency(RewardLimitsDTO.RewardLimitFrequency.DAILY)
                                                        .rewardLimit(BigDecimal.valueOf(40))
                                                        .build(),
                                                RewardLimitsDTO.builder()
                                                        .frequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY)
                                                        .rewardLimit(BigDecimal.valueOf(200))
                                                        .build(),
                                                RewardLimitsDTO.builder()
                                                        .frequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY)
                                                        .rewardLimit(BigDecimal.valueOf(1000))
                                                        .build(),
                                                RewardLimitsDTO.builder()
                                                        .frequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY)
                                                        .rewardLimit(BigDecimal.valueOf(10000))
                                                        .build()
                                        ))
                                        .build())
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(5, Collections.emptySet(), RewardValueDTO.class)
                                .initiativeId(INITIATIVE_ID_EXHAUSTED)
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(6, Collections.emptySet(), RewardValueDTO.class)
                                .initiativeId(INITIATIVE_ID_EXHAUSTING)
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .general(InitiativeGeneralDTO.builder()
                                        .budget(BigDecimal.valueOf(1000))
                                        .build())
                                .build()
                )
                .peek(i -> expectedRules[0] += RewardRuleConsumerConfigTest.calcDroolsRuleGenerated(i))
                .forEach(i -> publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, i));

        RewardRuleConsumerConfigTest.waitForKieContainerBuild(expectedRules[0], rewardContextHolderService);
    }
    //endregion

    private TransactionDTO mockInstance(int bias) {
        return useCases.get(bias % useCases.size()).getFirst().apply(bias);
    }

    private void checkResponse(RewardTransactionDTO rewardedTrx) {
        String hpan = rewardedTrx.getHpan();
        int biasRetrieve = Integer.parseInt(hpan.substring(4));
        useCases.get(biasRetrieve % useCases.size()).getSecond().accept(rewardedTrx);

    }

    //region useCases
    private final LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(0, 0));
    private final OffsetDateTime trxDate = OffsetDateTime.of(localDateTime, ZoneId.of("Europe/Rome").getRules().getOffset(localDateTime));

    private final Map<String, UserInitiativeCounters> expectedCounters = new HashMap<>();
    private final Map<String, BigDecimal> initiative2ExpectedReward = Map.of(
            INITIATIVE_ID_THRESHOLD_BASED, BigDecimal.valueOf(0.5),
            INITIATIVE_ID_DAYOFWEEK_BASED, BigDecimal.valueOf(5),
            INITIATIVE_ID_MCC_BASED, BigDecimal.valueOf(6),
            INITIATIVE_ID_REWARDLIMITS_BASED, BigDecimal.valueOf(0.8),
            INITIATIVE_ID_TRXCOUNT_BASED, BigDecimal.valueOf(7),
            INITIATIVE_ID_EXHAUSTED, BigDecimal.valueOf(0),
            INITIATIVE_ID_EXHAUSTING, BigDecimal.valueOf(1)
    );

    private final List<Pair<Function<Integer, TransactionDTO>, java.util.function.Consumer<RewardTransactionDTO>>> useCases = List.of(
            // rewarded by threshold based initiative
            Pair.of(
                    i -> onboardTrxHpanAndIncreaseCounters(
                            TransactionDTOFaker.mockInstanceBuilder(i)
                                    .amount(BigDecimal.valueOf(5))
                                    .build(),
                            INITIATIVE_ID_THRESHOLD_BASED),
                    evaluation -> assertRewardedState(evaluation, INITIATIVE_ID_THRESHOLD_BASED, false)
            ),
            // rewarded by dayOfWeek based initiative
            Pair.of(
                    i -> onboardTrxHpanAndIncreaseCounters(
                            TransactionDTOFaker.mockInstanceBuilder(i)
                                    .trxDate(trxDate)
                                    .amount(BigDecimal.valueOf(50))
                                    .build(),
                            INITIATIVE_ID_DAYOFWEEK_BASED),
                    evaluation -> assertRewardedState(evaluation, INITIATIVE_ID_DAYOFWEEK_BASED, false)
            ),
            // rewarded by MccFilter based initiative
            Pair.of(
                    i -> onboardTrxHpanAndIncreaseCounters(
                            TransactionDTOFaker.mockInstanceBuilder(i)
                                    .mcc("ACCEPTEDMCC")
                                    .amount(BigDecimal.valueOf(60))
                                    .build(),
                            INITIATIVE_ID_MCC_BASED),
                    evaluation -> assertRewardedState(evaluation, INITIATIVE_ID_MCC_BASED, false)
            ),
            // rewarded by TrxCount based initiative
            Pair.of(
                    i -> {
                        final TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                                .amount(BigDecimal.valueOf(70))
                                .build();
                        saveUserInitiativeCounter(trx, InitiativeCounters.builder()
                                .initiativeId(INITIATIVE_ID_TRXCOUNT_BASED)
                                .trxNumber(TRX_NUMBER_MIN_NUMBER_INITIATIVE_ID_TRXCOUNT)
                                .build(), INITIATIVE_ID_TRXCOUNT_BASED);
                        return onboardTrxHpanAndIncreaseCounters(
                                trx,
                                INITIATIVE_ID_TRXCOUNT_BASED);
                    },
                    evaluation -> assertRewardedState(evaluation, INITIATIVE_ID_TRXCOUNT_BASED, false)
            ),
            // rewarded by RewardLimits based initiative
            Pair.of(
                    i -> onboardTrxHpanAndIncreaseCounters(
                            TransactionDTOFaker.mockInstanceBuilder(i)
                                    .amount(BigDecimal.valueOf(8))
                                    .build(),
                            INITIATIVE_ID_REWARDLIMITS_BASED),
                    evaluation -> assertRewardedState(evaluation, INITIATIVE_ID_REWARDLIMITS_BASED, false)
            ),
            // rewarded by RewardLimits based initiative, daily capped
            buildRewardLimitsCappedUseCase(RewardLimitsDTO.RewardLimitFrequency.DAILY),
            // rewarded by RewardLimits based initiative, weekly capped
            buildRewardLimitsCappedUseCase(RewardLimitsDTO.RewardLimitFrequency.WEEKLY),
            // rewarded by RewardLimits based initiative, monthly capped
            buildRewardLimitsCappedUseCase(RewardLimitsDTO.RewardLimitFrequency.MONTHLY),
            // rewarded by RewardLimits based initiative, yearly capped
            buildRewardLimitsCappedUseCase(RewardLimitsDTO.RewardLimitFrequency.YEARLY),
            // not rewarded due to no initiatives
            Pair.of(
                    i -> {
                        TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
                        createUserCounter(trx);
                        return trx;
                    },
                    evaluation -> {
                        Assertions.assertFalse(evaluation.getRejectionReasons().isEmpty());
                        Assertions.assertEquals(Collections.emptyMap(), evaluation.getInitiativeRejectionReasons());
                        assertTrue(evaluation.getRewards().isEmpty());
                        Assertions.assertEquals(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE), evaluation.getRejectionReasons());
                        Assertions.assertEquals("REJECTED", evaluation.getStatus());
                    }
            ),
            // not rewarded
            Pair.of(
                    i -> {
                        TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                                .trxDate(trxDate.minusDays(1))
                                .amount(BigDecimal.ONE)
                                .mcc("NOTALLOWED")
                                .build();

                        final InitiativeCounters initiativeRewardCounter = InitiativeCounters.builder()
                                .initiativeId(INITIATIVE_ID_REWARDLIMITS_BASED)
                                .dailyCounters(new HashMap<>(Map.of("2021-12-31", Counters.builder().totalReward(BigDecimal.valueOf(40)).build())))
                                .weeklyCounters(new HashMap<>(Map.of("2021-12-5", Counters.builder().totalReward(BigDecimal.valueOf(200)).build())))
                                .monthlyCounters(new HashMap<>(Map.of("2021-12", Counters.builder().totalReward(BigDecimal.valueOf(1000)).build())))
                                .yearlyCounters(new HashMap<>(Map.of("2021", Counters.builder().totalReward(BigDecimal.valueOf(10000)).build())))
                                .build();
                        saveUserInitiativeCounter(trx, initiativeRewardCounter, INITIATIVE_ID_REWARDLIMITS_BASED);

                        final UserInitiativeCounters userInitiativeCounters = onboardTrxHPan(
                                trx,
                                INITIATIVE_ID_THRESHOLD_BASED,
                                INITIATIVE_ID_DAYOFWEEK_BASED,
                                INITIATIVE_ID_MCC_BASED,
                                INITIATIVE_ID_TRXCOUNT_BASED,
                                INITIATIVE_ID_REWARDLIMITS_BASED
                        );
                        userInitiativeCounters.getInitiatives().put(INITIATIVE_ID_REWARDLIMITS_BASED, initiativeRewardCounter);
                        return trx;
                    },
                    evaluation -> assertRejectedInitiativesState(evaluation,
                            Map.of(
                                    INITIATIVE_ID_DAYOFWEEK_BASED, List.of(RewardConstants.InitiativeTrxConditionOrder.DAYOFWEEK.getRejectionReason()),
                                    INITIATIVE_ID_THRESHOLD_BASED, List.of(RewardConstants.InitiativeTrxConditionOrder.THRESHOLD.getRejectionReason()),
                                    INITIATIVE_ID_TRXCOUNT_BASED, List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason()),
                                    INITIATIVE_ID_MCC_BASED, List.of(RewardConstants.InitiativeTrxConditionOrder.MCCFILTER.getRejectionReason()),
                                    INITIATIVE_ID_REWARDLIMITS_BASED, List.of(
                                            RewardConstants.InitiativeTrxConditionOrder.REWARDLIMITS.getRejectionReason().formatted(RewardLimitsDTO.RewardLimitFrequency.DAILY),
                                            RewardConstants.InitiativeTrxConditionOrder.REWARDLIMITS.getRejectionReason().formatted(RewardLimitsDTO.RewardLimitFrequency.WEEKLY),
                                            RewardConstants.InitiativeTrxConditionOrder.REWARDLIMITS.getRejectionReason().formatted(RewardLimitsDTO.RewardLimitFrequency.MONTHLY),
                                            RewardConstants.InitiativeTrxConditionOrder.REWARDLIMITS.getRejectionReason().formatted(RewardLimitsDTO.RewardLimitFrequency.YEARLY)
                                    )
                            ),
                            Collections.emptyList())
            ),
            // useCase initiative budget exhausted
            Pair.of(
                    i -> {
                        TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                                .amount(BigDecimal.TEN)
                                .build();

                        final InitiativeCounters initiativeRewardCounter = InitiativeCounters.builder()
                                .initiativeId(INITIATIVE_ID_EXHAUSTED)
                                .exhaustedBudget(true)
                                .build();
                        saveUserInitiativeCounter(trx, initiativeRewardCounter, INITIATIVE_ID_EXHAUSTED);

                        final UserInitiativeCounters userInitiativeCounters = onboardTrxHPan(
                                trx,
                                INITIATIVE_ID_EXHAUSTED
                        );
                        userInitiativeCounters.getInitiatives().put(INITIATIVE_ID_EXHAUSTED, initiativeRewardCounter);
                        return trx;
                    },
                    evaluation -> assertRejectedInitiativesState(evaluation,
                            Map.of(
                                    INITIATIVE_ID_EXHAUSTED, List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)
                            ),
                            List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
            ),
            // useCase exhausting initiative budget
            Pair.of(
                    i -> {
                        TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                                .amount(BigDecimal.valueOf(100))
                                .build();

                        final InitiativeCounters initiativeRewardCounter = InitiativeCounters.builder()
                                .initiativeId(INITIATIVE_ID_EXHAUSTING)
                                .totalReward(BigDecimal.valueOf(999))
                                .build();
                        saveUserInitiativeCounter(trx, initiativeRewardCounter, INITIATIVE_ID_EXHAUSTING);

                        final UserInitiativeCounters userInitiativeCounters = onboardTrxHPan(
                                trx,
                                INITIATIVE_ID_EXHAUSTING
                        );
                        userInitiativeCounters.getInitiatives().put(INITIATIVE_ID_EXHAUSTING, initiativeRewardCounter);
                        updateCounters(initiativeRewardCounter, trx, BigDecimal.valueOf(1));
                        initiativeRewardCounter.setExhaustedBudget(true);
                        return trx;
                    },
                    evaluation -> {
                        assertRewardedState(evaluation, INITIATIVE_ID_EXHAUSTING, true);
                        Reward reward = evaluation.getRewards().get(INITIATIVE_ID_EXHAUSTING);
                        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(10), reward.getProvidedReward());
                        assertTrue(reward.isCapped());
                    }
            )
    );

    private Pair<Function<Integer, TransactionDTO>, java.util.function.Consumer<RewardTransactionDTO>> buildRewardLimitsCappedUseCase(RewardLimitsDTO.RewardLimitFrequency frequencyCapped) {
        final boolean isDailyCapped = RewardLimitsDTO.RewardLimitFrequency.DAILY.equals(frequencyCapped);
        final boolean isWeeklyCapped = RewardLimitsDTO.RewardLimitFrequency.WEEKLY.equals(frequencyCapped);
        final boolean isMonthlyCapped = RewardLimitsDTO.RewardLimitFrequency.MONTHLY.equals(frequencyCapped);
        final boolean isYearlyCapped = RewardLimitsDTO.RewardLimitFrequency.YEARLY.equals(frequencyCapped);

        return Pair.of(
                i -> {
                    final TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                            .trxDate(trxDate)
                            .amount(BigDecimal.valueOf(80))
                            .build();

                    final InitiativeCounters initialStateOfCounters = InitiativeCounters.builder()
                            .initiativeId(INITIATIVE_ID_REWARDLIMITS_BASED)
                            .dailyCounters(new HashMap<>(Map.of("2022-01-01", Counters.builder().totalReward(BigDecimal.valueOf(isDailyCapped ? 39.2 : 32)).build())))
                            .weeklyCounters(new HashMap<>(Map.of("2022-01-0", Counters.builder().totalReward(BigDecimal.valueOf(isWeeklyCapped ? 199.2 : 199)).build())))
                            .monthlyCounters(new HashMap<>(Map.of("2022-01", Counters.builder().totalReward(BigDecimal.valueOf(isMonthlyCapped ? 999.2 : 999)).build())))
                            .yearlyCounters(new HashMap<>(Map.of("2022", Counters.builder().totalReward(BigDecimal.valueOf(isYearlyCapped ? 9999.2 : 9999)).build())))
                            .build();

                    saveUserInitiativeCounter(trx, initialStateOfCounters, INITIATIVE_ID_REWARDLIMITS_BASED);
                    createUserCounter(trx).getInitiatives().put(INITIATIVE_ID_REWARDLIMITS_BASED, initialStateOfCounters);

                    return onboardTrxHpanAndIncreaseCounters(
                            trx,
                            INITIATIVE_ID_REWARDLIMITS_BASED);
                },
                evaluation -> {
                    try {
                        assertRewardedState(evaluation, INITIATIVE_ID_REWARDLIMITS_BASED, true);

                        final Reward initiativeReward = evaluation.getRewards().get(INITIATIVE_ID_REWARDLIMITS_BASED);

                        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(8), initiativeReward.getProvidedReward());
                        assertEquals(List.of(
                                        isDailyCapped,
                                        isWeeklyCapped,
                                        isMonthlyCapped,
                                        isYearlyCapped
                                ),
                                List.of(
                                        initiativeReward.isDailyCapped(),
                                        initiativeReward.isWeeklyCapped(),
                                        initiativeReward.isMonthlyCapped(),
                                        initiativeReward.isYearlyCapped()
                                ));
                    } catch (Error e) {
                        System.err.printf("There were errors asserting %s frequency capped%n", frequencyCapped);
                        throw e;
                    }
                }
        );
    }

    private void assertRewardedState(RewardTransactionDTO evaluation, String rewardedInitiativeId, boolean expectedCap) {
        Assertions.assertEquals(Collections.emptyList(), evaluation.getRejectionReasons());
        Assertions.assertEquals(Collections.emptyMap(), evaluation.getInitiativeRejectionReasons());
        Assertions.assertFalse(evaluation.getRewards().isEmpty());
        Assertions.assertEquals("REWARDED", evaluation.getStatus());

        final Reward initiativeReward = evaluation.getRewards().get(rewardedInitiativeId);
        Assertions.assertNotNull(initiativeReward);

        TestUtils.assertBigDecimalEquals(initiative2ExpectedReward.get(rewardedInitiativeId), initiativeReward.getAccruedReward());
        if (!expectedCap) {
            TestUtils.assertBigDecimalEquals(initiativeReward.getProvidedReward(), initiativeReward.getAccruedReward());
        }
    }

    private void assertRejectedInitiativesState(RewardTransactionDTO evaluation, Map<String, List<String>> expectedInitiativeRejectionReasons, List<String> expectedRejectionReasons) {
        Assertions.assertEquals(Collections.emptyMap(), evaluation.getRewards());
        Assertions.assertEquals(expectedRejectionReasons, evaluation.getRejectionReasons());
        Assertions.assertFalse(evaluation.getInitiativeRejectionReasons().isEmpty());
        Assertions.assertEquals(expectedInitiativeRejectionReasons, evaluation.getInitiativeRejectionReasons());
        Assertions.assertEquals("REJECTED", evaluation.getStatus());
    }

    private TransactionDTO onboardTrxHpanAndIncreaseCounters(TransactionDTO trx, String... initiativeIds) {
        UserInitiativeCounters userInitiativeCounters = onboardTrxHPan(trx, initiativeIds);

        Arrays.stream(initiativeIds).forEach(id -> {
            InitiativeConfig initiativeConfig = Objects.requireNonNull(droolsRuleRepository.findById(id).block()).getInitiativeConfig();
            // the use case of the initiative INITIATIVE_ID_TRXCOUNT_BASED start with a count of 9 trx
            if (INITIATIVE_ID_TRXCOUNT_BASED.equals(id)) {
                userInitiativeCounters.getInitiatives().computeIfAbsent(
                        INITIATIVE_ID_TRXCOUNT_BASED,
                        i -> InitiativeCounters.builder()
                                .initiativeId(INITIATIVE_ID_TRXCOUNT_BASED)
                                .trxNumber(TRX_NUMBER_MIN_NUMBER_INITIATIVE_ID_TRXCOUNT)
                                .build()
                );
            }
            updateInitiativeCounters(userInitiativeCounters
                            .getInitiatives().computeIfAbsent(id, x -> InitiativeCounters.builder().initiativeId(id).build()),
                    trx, initiative2ExpectedReward.get(id), initiativeConfig);
        });

        return trx;
    }

    private UserInitiativeCounters onboardTrxHPan(TransactionDTO trx, String... initiativeIds) {
        hpanInitiativesRepository.save(HpanInitiatives.builder()
                .hpan(trx.getHpan())
                .onboardedInitiatives(Arrays.stream(initiativeIds).map(initiativeId -> OnboardedInitiative.builder()
                                .initiativeId(initiativeId)
                                .activeTimeIntervals(List.of(ActiveTimeInterval.builder()
                                        .startInterval(trx.getTrxDate().toLocalDateTime())
                                        .endInterval(trx.getTrxDate().toLocalDateTime())
                                        .build()))
                                .build())
                        .collect(Collectors.toList()))
                .build()).block();


        return createUserCounter(trx);
    }

    private UserInitiativeCounters createUserCounter(TransactionDTO trx) {
        return expectedCounters.computeIfAbsent(trx.getUserId(), u -> new UserInitiativeCounters(u, new HashMap<>()));
    }

    private void saveUserInitiativeCounter(TransactionDTO trx, InitiativeCounters initiativeRewardCounter, String initiativeIdExhausted) {
        userInitiativeCountersRepository.save(UserInitiativeCounters.builder()
                .userId(trx.getUserId())
                .initiatives(new HashMap<>(Map.of(
                        initiativeIdExhausted,
                        initiativeRewardCounter
                )))
                .build()).block();
    }

    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter weekFormatter = DateTimeFormatter.ofPattern("yyyy-MM-W", Locale.ITALY);
    private final DateTimeFormatter monthlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    private final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");

    private void updateInitiativeCounters(InitiativeCounters counters, TransactionDTO trx, BigDecimal expectedReward, InitiativeConfig initiativeConfig) {
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

    private void updateCounters(Counters counters, TransactionDTO trx, BigDecimal expectedReward) {
        counters.setTrxNumber(counters.getTrxNumber() + 1);
        counters.setTotalAmount(counters.getTotalAmount().add(trx.getAmount()).setScale(2, RoundingMode.UNNECESSARY));
        counters.setTotalReward(counters.getTotalReward().add(expectedReward).setScale(2, RoundingMode.UNNECESSARY));
    }
    //endregion

    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp getErrorUseCaseIdPatternMatch
    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"correlationId\":\"CORRELATIONID([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotExpected = "{\"correlationId\":\"CORRELATIONID0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"correlationId\":\"CORRELATIONID1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "Unexpected JSON", jsonNotValid)
        ));

        final String failingRuleEngineUserId = "FAILING_RULE_ENGINE";
        String failingRuleEngineUseCase = TestUtils.jsonSerializer(
                TransactionDTOFaker.mockInstanceBuilder(errorUseCases.size())
                        .userId(failingRuleEngineUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(ruleEngineServiceSpy).applyRules(Mockito.argThat(i->failingRuleEngineUserId.equals(i.getUserId())), Mockito.any(), Mockito.any());
                    return failingRuleEngineUseCase;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "An error occurred evaluating transaction", failingRuleEngineUseCase)
        ));

        final String failingCounterUpdateUserId = "FAILING_COUNTER_UPDATE";
        String failingCounterUpdate = TestUtils.jsonSerializer(
                TransactionDTOFaker.mockInstanceBuilder(errorUseCases.size())
                        .userId(failingCounterUpdateUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(userInitiativeCountersUpdateServiceSpy).update(Mockito.any(), Mockito.argThat(i->failingCounterUpdateUserId.equals(i.getUserId())));
                    return failingCounterUpdate;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "An error occurred evaluating transaction", failingCounterUpdate)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicRewardProcessorRequest, errorMessage, errorDescription, expectedPayload);
    }
    //endregion
}