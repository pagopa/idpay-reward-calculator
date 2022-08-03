package it.gov.pagopa.reward.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.BaseIntegrationTest;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.*;
import it.gov.pagopa.reward.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.model.ActiveTimeInterval;
import it.gov.pagopa.reward.model.HpanInitiatives;
import it.gov.pagopa.reward.model.OnboardedInitiative;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.repository.DroolsRuleRepository;
import it.gov.pagopa.reward.repository.HpanInitiativesRepository;
import it.gov.pagopa.reward.repository.UserInitiativeCountersRepository;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = {
        "app.reward-rule.build-delay-duration=PT1S",
        "app.rules.cache.refresh-ms-rate=60000",
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
    private DroolsRuleRepository droolsRuleRepository;

    @Autowired
    private UserInitiativeCountersRepository userInitiativeCountersRepository;

    @Test
    void testTransactionProcessor() throws JsonProcessingException {
        int N = 1000;
        long maxWaitingMs = 30000;

        publishRewardRules();

        List<TransactionDTO> trxs = IntStream.range(0, N)
                .mapToObj(this::mockInstance).toList();

        long timePublishOnboardingStart = System.currentTimeMillis();
        trxs.forEach(i -> publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, i.getHpan(), i)); // TODO use userId
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topicRewardProcessorOutcome, "idpay-group");

        long timeConsumerResponse = System.currentTimeMillis();

        List<String> payloadConsumed = new ArrayList<>(N);
        int counter = 0;
        while (counter < N) {
            if (System.currentTimeMillis() - timeConsumerResponse > maxWaitingMs) {
                Assertions.fail("timeout of %d ms expired. Read %d while expected %d messages".formatted(maxWaitingMs, counter, N));
            }
            ConsumerRecords<String, String> published = consumer.poll(Duration.ofMillis(7000));
            for (ConsumerRecord<String, String> record : published) {
                payloadConsumed.add(record.value());
                counter++;
            }
        }
        long timeEnd = System.currentTimeMillis();
        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(N, counter);
        for (String p : payloadConsumed) {
            checkResponse(objectMapper.readValue(p, RewardTransactionDTO.class));
        }

        Assertions.assertEquals(
                objectMapper.writeValueAsString(expectedCounters.values().stream()
                        .sorted(Comparator.comparing(UserInitiativeCounters::getUserId))
                        .toList()),
                objectMapper.writeValueAsString(Objects.requireNonNull(userInitiativeCountersRepository.findAll().collectList().block()).stream()
                        .sorted(Comparator.comparing(UserInitiativeCounters::getUserId))
                        .toList()));

        System.out.printf("""
                        ************************
                        Time spent to send %d trx messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                N, timePublishingOnboardingRequest,
                timeConsumerResponseEnd,
                timeEnd - timePublishOnboardingStart
        );
    }

    // region initiative build
    private static final String INITIATIVE_ID_THRESHOLD_BASED = "ID_0_THRESHOLD";
    private static final String INITIATIVE_ID_DAYOFWEEK_BASED = "ID_1_DAYOFWEEK";
    private static final String INITIATIVE_ID_MCC_BASED = "ID_2_MCC";
    private static final String INITIATIVE_ID_TRXCOUNT_BASED = "ID_3_TRXCOUNT";
    private static final String INITIATIVE_ID_REWARDLIMITS_BASED = "ID_4_REWARDLIMITS";

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

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), "Expected: %s, Obtained: %s".formatted(expected, actual));
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
            INITIATIVE_ID_TRXCOUNT_BASED, BigDecimal.valueOf(7)
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
                        userInitiativeCountersRepository.save(UserInitiativeCounters.builder()
                                .userId(trx.getHpan()) //TODO use userId
                                .initiatives(new HashMap<>(Map.of(
                                        INITIATIVE_ID_TRXCOUNT_BASED,
                                        InitiativeCounters.builder()
                                                .initiativeId(INITIATIVE_ID_TRXCOUNT_BASED)
                                                .trxNumber(TRX_NUMBER_MIN_NUMBER_INITIATIVE_ID_TRXCOUNT)
                                                .build()
                                )))
                                .build()).block();
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
            // not rewarded hpan not onboarded
            Pair.of(
                    i -> {
                        TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
                        createUserCounter(trx);
                        return trx;
                    },
                    evaluation -> {
                        Assertions.assertFalse(evaluation.getRejectionReasons().isEmpty());
                        Assertions.assertEquals(Collections.emptyMap(), evaluation.getInitiativeRejectionReasons());
                        Assertions.assertTrue(evaluation.getRewards().isEmpty());
                        Assertions.assertEquals(List.of("HPAN_NOT_ACTIVE"), evaluation.getRejectionReasons());
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
                        userInitiativeCountersRepository.save(UserInitiativeCounters.builder()
                                .userId(trx.getHpan()) //TODO use userId
                                .initiatives(new HashMap<>(Map.of(
                                        INITIATIVE_ID_REWARDLIMITS_BASED,
                                        initiativeRewardCounter
                                )))
                                .build()).block();

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
                            ))
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

                    userInitiativeCountersRepository.save(UserInitiativeCounters.builder()
                            .userId(trx.getHpan()) //TODO use userId
                            .initiatives(new HashMap<>(Map.of(
                                    INITIATIVE_ID_REWARDLIMITS_BASED,
                                    initialStateOfCounters
                            )))
                            .build()).block();
                    createUserCounter(trx).getInitiatives().put(INITIATIVE_ID_REWARDLIMITS_BASED, initialStateOfCounters);

                    return onboardTrxHpanAndIncreaseCounters(
                            trx,
                            INITIATIVE_ID_REWARDLIMITS_BASED);
                },
                evaluation -> {
                    try {
                        assertRewardedState(evaluation, INITIATIVE_ID_REWARDLIMITS_BASED, true);

                        final Reward initiativeReward = evaluation.getRewards().get(INITIATIVE_ID_REWARDLIMITS_BASED);

                        assertBigDecimalEquals(BigDecimal.valueOf(8), initiativeReward.getProvidedReward());
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

        assertBigDecimalEquals(initiative2ExpectedReward.get(rewardedInitiativeId), initiativeReward.getAccruedReward());
        if (!expectedCap) {
            assertBigDecimalEquals(initiativeReward.getProvidedReward(), initiativeReward.getAccruedReward());
        }
    }

    private void assertRejectedInitiativesState(RewardTransactionDTO evaluation, Map<String, List<String>> expectedRejectedReasons) {
        Assertions.assertEquals(Collections.emptyMap(), evaluation.getRewards());
        Assertions.assertEquals(Collections.emptyList(), evaluation.getRejectionReasons());
        Assertions.assertFalse(evaluation.getInitiativeRejectionReasons().isEmpty());
        Assertions.assertEquals(expectedRejectedReasons, evaluation.getInitiativeRejectionReasons());
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
        return expectedCounters.computeIfAbsent(trx.getHpan(), u -> new UserInitiativeCounters(u, new HashMap<>()));//TODO use userId
    }

    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter weekFormatter = DateTimeFormatter.ofPattern("yyyy-MM-W", Locale.ITALY);
    private final DateTimeFormatter monthlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    private final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");

    private void updateInitiativeCounters(InitiativeCounters counters, TransactionDTO trx, BigDecimal expectedReward, InitiativeConfig initiativeConfig) {
        updateCounters(counters, trx, expectedReward);
        if (initiativeConfig.isHasDailyThreshold()) {
            updateCounters(
                    counters.getDailyCounters().computeIfAbsent(trx.getTrxDate().format(dayFormatter), d -> new Counters()),
                    trx, expectedReward);
        }
        if (initiativeConfig.isHasWeeklyThreshold()) {
            updateCounters(
                    counters.getWeeklyCounters().computeIfAbsent(trx.getTrxDate().format(weekFormatter), d -> new Counters()),
                    trx, expectedReward);
        }
        if (initiativeConfig.isHasMonthlyThreshold()) {
            updateCounters(
                    counters.getMonthlyCounters().computeIfAbsent(trx.getTrxDate().format(monthlyFormatter), d -> new Counters()),
                    trx, expectedReward);
        }
        if (initiativeConfig.isHasYearlyThreshold()) {
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
}