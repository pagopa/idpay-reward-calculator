package it.gov.pagopa.reward.connector.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.connector.event.consumer.RewardRuleConsumerConfigTest;
import it.gov.pagopa.reward.controller.InstrumentApiControllerIntegrationTest;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.Transaction2RewardTransactionMapper;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.*;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.RewardNotifierService;
import it.gov.pagopa.reward.service.reward.evaluate.RuleEngineService;
import it.gov.pagopa.reward.service.reward.evaluate.UserInitiativeCountersUpdateService;
import it.gov.pagopa.reward.service.reward.trx.TransactionProcessedService;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import it.gov.pagopa.reward.utils.Utils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(
        properties = {
                "logging.level.it.gov.pagopa.common.web.exception.ErrorManager=OFF",
                "logging.level.it.gov.pagopa.common.reactive.utils.PerformanceLogger=WARN",
                "logging.level.it.gov.pagopa.reward.controller.InstrumentApiControllerImpl=WARN"
        })
class TransactionProcessorTest extends BaseTransactionProcessorTest {

    public static final long TRX_NUMBER_MIN_NUMBER_INITIATIVE_ID_TRXCOUNT = 9L;
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

    private TransactionDTO trxDuplicated;

    @Test
    void testTransactionProcessor() throws JsonProcessingException {
        int validTrx = 1000; // use even number
        int notValidTrx = errorUseCases.size();
        int duplicateTrx = Math.min(100, validTrx/2); // we are sending as duplicated the first N transactions: error cases could invalidate duplicate check
        long maxWaitingMs = 60000;

        publishRewardRules();

        trxDuplicated = TransactionDTOFaker.mockInstance(1);
        trxDuplicated.setOperationTypeTranscoded(OperationType.CHARGE);
        trxDuplicated.setAmount(TestUtils.bigDecimalValue(1));
        trxDuplicated.setAmountCents(1_00L);
        trxDuplicated.setEffectiveAmount(trxDuplicated.getAmount());
        trxDuplicated.setTrxChargeDate(trxDuplicated.getTrxDate());
        trxDuplicated.setIdTrxAcquirer("ALREADY_PROCESSED_TRX");
        transactionProcessedService.save(transaction2RewardTransactionMapper.apply(trxDuplicated)).block();

        List<String> trxs = new ArrayList<>(buildValidPayloads(errorUseCases.size(), validTrx / 2));
        trxs.addAll(IntStream.range(0, notValidTrx).mapToObj(i -> errorUseCases.get(i).getFirst().get()).toList());
        trxs.addAll(buildValidPayloads(errorUseCases.size() + (validTrx / 2) + notValidTrx, validTrx / 2));

        trxs.add(objectMapper.writeValueAsString(trxDuplicated));
        int alreadyProcessed = 1;

        long totalSendMessages = trxs.size()+duplicateTrx;

        long timePublishOnboardingStart = System.currentTimeMillis();
        int[] i=new int[]{0};
        trxs.forEach(p -> {
            final String userId = Utils.readUserId(p);
            kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, userId,p);

            // to test duplicate trx and their right processing order
            if(i[0]<duplicateTrx){
                i[0]++;
                kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, userId, p.replaceFirst("(senderCode\":\"[^\"]+)", "$1%s".formatted(DUPLICATE_SUFFIX)));
            }
        });
        // to test trx sent from other application
        kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardProcessorRequest, List.of(new RecordHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = kafkaTestUtilitiesService.consumeMessages(topicRewardProcessorOutcome, validTrx, maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(validTrx, payloadConsumed.size());
        Assertions.assertEquals(validTrx+3, // +1 due to pre-existent, +2 because stored in publish error useCases
                transactionProcessedRepository.count().block());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            RewardTransactionDTO payload = objectMapper.readValue(p.value(), RewardTransactionDTO.class);
            checkResponse(payload);
            Assertions.assertEquals(payload.getUserId(), p.key());
        }

        assertCounters();

        checkErrorsPublished(notValidTrx, maxWaitingMs, errorUseCases);

        System.out.printf("""
                        ************************
                        Time spent to send %d (%d + %d + %d + %d) trx messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                totalSendMessages,
                validTrx,
                duplicateTrx,
                notValidTrx,
                alreadyProcessed,
                timePublishingOnboardingRequest,
                timeConsumerResponseEnd,
                timeEnd - timePublishOnboardingStart
        );

        checkOffsets(totalSendMessages+1, validTrx); // +1 due to other applicationName useCase
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
    private static final String INITIATIVE_ID_EXPIRED = "ID_7_EXPIRED";
    private static final String INITIATIVE_ID_NOT_STARTED = "ID_8_NOTSTARTED";
    private static final String INITIATIVE_ID_INSTRUMENT_INACTIVE = "ID_9_INSTRUMENTINACTIVE";


    private void publishRewardRules() {
        int[] expectedRules = {0};
        Stream.of(
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(0, Collections.emptySet(), RewardValueDTO.class)
                                .initiativeId(INITIATIVE_ID_THRESHOLD_BASED)
                                .initiativeName("NAME_"+INITIATIVE_ID_THRESHOLD_BASED)
                                .organizationId("ORGANIZATIONID_"+INITIATIVE_ID_THRESHOLD_BASED)
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
                                .initiativeName("NAME_"+INITIATIVE_ID_DAYOFWEEK_BASED)
                                .organizationId("ORGANIZATIONID_"+INITIATIVE_ID_DAYOFWEEK_BASED)
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
                                .initiativeName("NAME_"+INITIATIVE_ID_MCC_BASED)
                                .organizationId("ORGANIZATIONID_"+INITIATIVE_ID_MCC_BASED)
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
                                .initiativeId(INITIATIVE_ID_TRXCOUNT_BASED)
                                .initiativeName("NAME_"+INITIATIVE_ID_TRXCOUNT_BASED)
                                .organizationId("ORGANIZATIONID_"+INITIATIVE_ID_TRXCOUNT_BASED)
                                .trxRule(InitiativeTrxConditions.builder()
                                        .trxCount(TrxCountDTO.builder()
                                                .from(10L)
                                                .fromIncluded(true)
                                                .build())
                                        .build())
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(3, Collections.emptySet(), RewardValueDTO.class)
                                .initiativeId(INITIATIVE_ID_REWARDLIMITS_BASED)
                                .initiativeName("NAME_"+INITIATIVE_ID_REWARDLIMITS_BASED)
                                .organizationId("ORGANIZATIONID_"+INITIATIVE_ID_REWARDLIMITS_BASED)
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
                                .initiativeName("NAME_"+INITIATIVE_ID_EXHAUSTED)
                                .organizationId("ORGANIZATIONID_"+INITIATIVE_ID_EXHAUSTED)
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(6, Collections.emptySet(), RewardValueDTO.class)
                                .initiativeId(INITIATIVE_ID_EXHAUSTING)
                                .initiativeName("NAME_"+INITIATIVE_ID_EXHAUSTING)
                                .organizationId("ORGANIZATIONID_"+INITIATIVE_ID_EXHAUSTING)
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .general(InitiativeGeneralDTO.builder()
                                        .budget(BigDecimal.valueOf(1000))
                                        .beneficiaryBudget(BigDecimal.valueOf(1000))
                                        .build())
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(6, Collections.emptySet(), null)
                                .initiativeId(INITIATIVE_ID_EXPIRED)
                                .initiativeName("NAME_"+INITIATIVE_ID_EXPIRED)
                                .organizationId("ORGANIZATIONID_"+INITIATIVE_ID_EXPIRED)
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .general(InitiativeGeneralDTO.builder()
                                        .endDate(trxDate.minusDays(2).toLocalDate())
                                        .build())
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(6, Collections.emptySet(), null)
                                .initiativeId(INITIATIVE_ID_NOT_STARTED)
                                .initiativeName("NAME_"+INITIATIVE_ID_NOT_STARTED)
                                .organizationId("ORGANIZATIONID_"+INITIATIVE_ID_NOT_STARTED)
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .general(InitiativeGeneralDTO.builder()
                                        .startDate(trxDate.plusYears(1L).toLocalDate())
                                        .build())
                                .build(),
                        InitiativeReward2BuildDTOFaker.mockInstanceBuilder(6, Collections.emptySet(), RewardValueDTO.class)
                                .initiativeId(INITIATIVE_ID_INSTRUMENT_INACTIVE)
                                .initiativeName("NAME_"+INITIATIVE_ID_INSTRUMENT_INACTIVE)
                                .organizationId("ORGANIZATIONID_"+INITIATIVE_ID_INSTRUMENT_INACTIVE)
                                .trxRule(InitiativeTrxConditions.builder()
                                        .threshold(ThresholdDTO.builder()
                                                .from(BigDecimal.valueOf(5))
                                                .fromIncluded(true)
                                                .build())
                                        .build())
                                .rewardRule(RewardValueDTO.builder()
                                        .rewardValue(BigDecimal.TEN)
                                        .build())
                                .build()
                )
                .peek(i -> expectedRules[0] += RewardRuleConsumerConfigTest.calcDroolsRuleGenerated(i))
                .forEach(i -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardRuleConsumer, null, null, i));

        RewardRuleConsumerConfigTest.waitForKieContainerBuild(expectedRules[0], rewardContextHolderService);
    }
    //endregion

    private TransactionDTO mockInstance(int bias) {
        int useCase = bias % useCases.size();
        final TransactionDTO trx = useCases.get(useCase).getFirst().apply(bias);
        onboardTrxHPanNoCreateUserCounter(trx, INITIATIVE_ID_EXPIRED, INITIATIVE_ID_NOT_STARTED, INITIATIVE_ID_INSTRUMENT_INACTIVE);

        InstrumentApiControllerIntegrationTest.cancelInstruments(webTestClient, trx.getUserId(), INITIATIVE_ID_INSTRUMENT_INACTIVE);

        return trx;
    }

    private void checkResponse(RewardTransactionDTO rewardedTrx) {
        String hpan = rewardedTrx.getHpan();
        int biasRetrieve = Integer.parseInt(hpan.substring(4));
        useCases.get(biasRetrieve % useCases.size()).getSecond().accept(rewardedTrx);
        Assertions.assertFalse(rewardedTrx.getSenderCode().endsWith(DUPLICATE_SUFFIX), "Unexpected senderCode: " + rewardedTrx.getSenderCode());
        Assertions.assertEquals(CommonUtilities.centsToEuro(rewardedTrx.getAmountCents()), rewardedTrx.getAmount());
        Assertions.assertNotNull(rewardedTrx.getRuleEngineTopicPartition());
        Assertions.assertNotNull(rewardedTrx.getRuleEngineTopicOffset());
    }

    //region useCases
    private final LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(0, 0));
    private final OffsetDateTime trxDate = OffsetDateTime.of(localDateTime, CommonConstants.ZONEID.getRules().getOffset(localDateTime));

    private final Map<String, BigDecimal> initiative2ExpectedReward = Map.of(
            INITIATIVE_ID_THRESHOLD_BASED, TestUtils.bigDecimalValue(0.5),
            INITIATIVE_ID_DAYOFWEEK_BASED, TestUtils.bigDecimalValue(5),
            INITIATIVE_ID_MCC_BASED, TestUtils.bigDecimalValue(6),
            INITIATIVE_ID_REWARDLIMITS_BASED, TestUtils.bigDecimalValue(0.8),
            INITIATIVE_ID_TRXCOUNT_BASED, TestUtils.bigDecimalValue(7),
            INITIATIVE_ID_EXHAUSTED, TestUtils.bigDecimalValue(0),
            INITIATIVE_ID_EXHAUSTING, TestUtils.bigDecimalValue(1)
    );

    private final List<Pair<Function<Integer, TransactionDTO>, java.util.function.Consumer<RewardTransactionDTO>>> useCases = List.of(
            // useCase 0: rewarded by threshold based initiative
            Pair.of(
                    i -> onboardTrxHpanAndIncreaseCounters(
                            TransactionDTOFaker.mockInstanceBuilder(i)
                                    .amount(BigDecimal.valueOf(5_00))
                                    .build(),
                            INITIATIVE_ID_THRESHOLD_BASED),
                    evaluation -> assertRewardedState(evaluation, INITIATIVE_ID_THRESHOLD_BASED, false, 1L, 5, 0, false)
            ),
            // useCase 1: rewarded by dayOfWeek based initiative (testing amountCents)
            Pair.of(
                    i -> onboardTrxHpanAndIncreaseCounters(
                            TransactionDTOFaker.mockInstanceBuilder(i)
                                    .trxDate(trxDate)
                                    .amount(null)
                                    .amountCents(50_00L)
                                    .build(),
                            INITIATIVE_ID_DAYOFWEEK_BASED),
                    evaluation -> assertRewardedState(evaluation, INITIATIVE_ID_DAYOFWEEK_BASED, false, 1L, 50, 0, false)
            ),
            // useCase 2: rewarded by MccFilter based initiative (filled both amount and amountCents)
            Pair.of(
                    i -> onboardTrxHpanAndIncreaseCounters(
                            TransactionDTOFaker.mockInstanceBuilder(i)
                                    .mcc("ACCEPTEDMCC")
                                    .amountCents(60_00L)
                                    .amount(BigDecimal.valueOf(60))
                                    .build(),
                            INITIATIVE_ID_MCC_BASED),
                    evaluation -> assertRewardedState(evaluation, INITIATIVE_ID_MCC_BASED, false, 1, 60, 0, false)
            ),
            // useCase 3: rewarded by TrxCount based initiative (to proof the amountCents rewrite logic)
            Pair.of(
                    i -> {
                        final TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                                .amountCents(70_00L)
                                .amount(BigDecimal.ZERO)
                                .build();
                        saveUserInitiativeCounter(trx, UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID_TRXCOUNT_BASED)
                                .trxNumber(TRX_NUMBER_MIN_NUMBER_INITIATIVE_ID_TRXCOUNT)
                                .build());

                        return onboardTrxHpanAndIncreaseCounters(
                                trx,
                                INITIATIVE_ID_TRXCOUNT_BASED);
                    },
                    evaluation -> assertRewardedState(evaluation, INITIATIVE_ID_TRXCOUNT_BASED, false, TRX_NUMBER_MIN_NUMBER_INITIATIVE_ID_TRXCOUNT + 1, 70, 0, false)
            ),
            // useCase 4: rejected by TrxCount based initiative lower than min
            Pair.of(
                    i -> {
                        final TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                                .amount(BigDecimal.valueOf(70_00))
                                .build();
                        saveUserInitiativeCounter(trx, UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID_TRXCOUNT_BASED)
                                .trxNumber(TRX_NUMBER_MIN_NUMBER_INITIATIVE_ID_TRXCOUNT-1)
                                .build());

                        UserInitiativeCountersWrapper userInitiativeCountersWrapper = onboardTrxHPan(
                                trx,
                                INITIATIVE_ID_TRXCOUNT_BASED);

                        userInitiativeCountersWrapper.getInitiatives().put(INITIATIVE_ID_TRXCOUNT_BASED,
                                UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID_TRXCOUNT_BASED)
                                        .version(1L)
                                        .trxNumber(TRX_NUMBER_MIN_NUMBER_INITIATIVE_ID_TRXCOUNT)
                                        .build());

                        return trx;
                    },
                    evaluation -> assertRejectedInitiativesState(evaluation,
                            Map.of(INITIATIVE_ID_TRXCOUNT_BASED, List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason())),
                            Collections.emptyList())
            ),
            // useCase 5: rewarded by RewardLimits based initiative
            Pair.of(
                    i -> onboardTrxHpanAndIncreaseCounters(
                            TransactionDTOFaker.mockInstanceBuilder(i)
                                    .amount(BigDecimal.valueOf(8_00))
                                    .build(),
                            INITIATIVE_ID_REWARDLIMITS_BASED),
                    evaluation -> assertRewardedState(evaluation, INITIATIVE_ID_REWARDLIMITS_BASED, false, 1, 8, 0, false)
            ),
            // useCase 6: rewarded by RewardLimits based initiative, daily capped
            buildRewardLimitsCappedUseCase(RewardLimitsDTO.RewardLimitFrequency.DAILY),
            // useCase 7: rewarded by RewardLimits based initiative, weekly capped
            buildRewardLimitsCappedUseCase(RewardLimitsDTO.RewardLimitFrequency.WEEKLY),
            // useCase 8: rewarded by RewardLimits based initiative, monthly capped
            buildRewardLimitsCappedUseCase(RewardLimitsDTO.RewardLimitFrequency.MONTHLY),
            // useCase 9: rewarded by RewardLimits based initiative, yearly capped
            buildRewardLimitsCappedUseCase(RewardLimitsDTO.RewardLimitFrequency.YEARLY),
            // useCase 10: not rewarded due to no initiatives
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
            // useCase 11: not rewarded
            Pair.of(
                    i -> {
                        TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                                .trxDate(trxDate.minusDays(1))
                                .amount(BigDecimal.valueOf(1_00))
                                .mcc("NOTALLOWED")
                                .build();

                        final UserInitiativeCounters initiativeRewardCounter = UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID_REWARDLIMITS_BASED)
                                .version(1L)
                                .dailyCounters(new HashMap<>(Map.of("2021-12-31", Counters.builder().totalReward(BigDecimal.valueOf(40)).build())))
                                .weeklyCounters(new HashMap<>(Map.of("2021-12-5", Counters.builder().totalReward(BigDecimal.valueOf(200)).build())))
                                .monthlyCounters(new HashMap<>(Map.of("2021-12", Counters.builder().totalReward(BigDecimal.valueOf(1000)).build())))
                                .yearlyCounters(new HashMap<>(Map.of("2021", Counters.builder().totalReward(BigDecimal.valueOf(10000)).build())))
                                .build();
                        saveUserInitiativeCounter(trx, initiativeRewardCounter);

                        final UserInitiativeCountersWrapper userInitiativeCountersWrapper = onboardTrxHPan(
                                trx,
                                INITIATIVE_ID_THRESHOLD_BASED,
                                INITIATIVE_ID_DAYOFWEEK_BASED,
                                INITIATIVE_ID_MCC_BASED,
                                INITIATIVE_ID_TRXCOUNT_BASED,
                                INITIATIVE_ID_REWARDLIMITS_BASED
                        );
                        userInitiativeCountersWrapper.getInitiatives().put(INITIATIVE_ID_REWARDLIMITS_BASED, initiativeRewardCounter);
                        userInitiativeCountersWrapper.getInitiatives().put(INITIATIVE_ID_TRXCOUNT_BASED,
                                UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID_TRXCOUNT_BASED)
                                        .version(1L)
                                        .trxNumber(1L)
                                        .build());
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
            // useCase 12: useCase initiative budget exhausted
            Pair.of(
                    i -> {
                        TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                                .amount(BigDecimal.valueOf(10_00))
                                .build();

                        final UserInitiativeCounters initiativeRewardCounter = UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID_EXHAUSTED)
                                .exhaustedBudget(true)
                                .build();
                        saveUserInitiativeCounter(trx, initiativeRewardCounter);

                        final UserInitiativeCountersWrapper userInitiativeCountersWrapper = onboardTrxHPan(
                                trx,
                                INITIATIVE_ID_EXHAUSTED
                        );
                        userInitiativeCountersWrapper.getInitiatives().put(INITIATIVE_ID_EXHAUSTED, initiativeRewardCounter);
                        return trx;
                    },
                    evaluation -> assertRejectedInitiativesState(evaluation,
                            Map.of(
                                    INITIATIVE_ID_EXHAUSTED, List.of(RewardConstants.INITIATIVE_REJECTION_REASON_BUDGET_EXHAUSTED)
                            ),
                            List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE))
            ),
            // useCase 13: useCase exhausting initiative budget
            Pair.of(
                    i -> {
                        TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                                .amount(BigDecimal.valueOf(100_00))
                                .build();

                        final UserInitiativeCounters initiativeRewardCounter = UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID_EXHAUSTING)
                                .totalReward(BigDecimal.valueOf(999))
                                .build();
                        saveUserInitiativeCounter(trx, initiativeRewardCounter);

                        final UserInitiativeCountersWrapper userInitiativeCountersWrapper = onboardTrxHPan(
                                trx,
                                INITIATIVE_ID_EXHAUSTING
                        );
                        userInitiativeCountersWrapper.getInitiatives().put(INITIATIVE_ID_EXHAUSTING, initiativeRewardCounter);
                        updateCounters(initiativeRewardCounter, trx, BigDecimal.valueOf(1));
                        initiativeRewardCounter.setVersion(1L);
                        initiativeRewardCounter.setExhaustedBudget(true);
                        return trx;
                    },
                    evaluation -> {
                        assertRewardedState(evaluation, INITIATIVE_ID_EXHAUSTING, true, 1, 100, 999, true);
                        Reward reward = evaluation.getRewards().get(INITIATIVE_ID_EXHAUSTING);
                        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(10), reward.getProvidedReward());
                        assertTrue(reward.isCapped());
                    }
            ),

            // useCase 14: discarded due to correlationId duplicated
            Pair.of(
                    i -> {
                        TransactionDTO trx = TransactionDTOFaker.mockInstanceBuilder(i)
                                .amount(BigDecimal.valueOf(5_00))
                                .acquirerId(trxDuplicated.getAcquirerId())
                                .correlationId(trxDuplicated.getCorrelationId())
                                .build();

                        onboardTrxHPanNoCreateUserCounter(trx,INITIATIVE_ID_THRESHOLD_BASED);

                        return trx;
                    },
                    evaluation -> assertRejectedInitiativesState(evaluation,
                            Collections.emptyMap(),
                            List.of(RewardConstants.TRX_REJECTION_REASON_DUPLICATE_CORRELATION_ID))
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
                            .amount(BigDecimal.valueOf(80_00))
                            .build();

                    final UserInitiativeCounters initialStateOfCounters = UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID_REWARDLIMITS_BASED)
                            .dailyCounters(new HashMap<>(Map.of("2022-01-01", Counters.builder().totalReward(BigDecimal.valueOf(isDailyCapped ? 39.2 : 32)).build())))
                            .weeklyCounters(new HashMap<>(Map.of("2022-01-0", Counters.builder().totalReward(BigDecimal.valueOf(isWeeklyCapped ? 199.2 : 199)).build())))
                            .monthlyCounters(new HashMap<>(Map.of("2022-01", Counters.builder().totalReward(BigDecimal.valueOf(isMonthlyCapped ? 999.2 : 999)).build())))
                            .yearlyCounters(new HashMap<>(Map.of("2022", Counters.builder().totalReward(BigDecimal.valueOf(isYearlyCapped ? 9999.2 : 9999)).build())))
                            .build();

                    saveUserInitiativeCounter(trx, initialStateOfCounters);
                    createUserCounter(trx).getInitiatives().put(INITIATIVE_ID_REWARDLIMITS_BASED, initialStateOfCounters);

                    return onboardTrxHpanAndIncreaseCounters(
                            trx,
                            INITIATIVE_ID_REWARDLIMITS_BASED);
                },
                evaluation -> {
                    try {
                        assertRewardedState(evaluation, INITIATIVE_ID_REWARDLIMITS_BASED, true, 1, 80, 0, false);

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

    private void assertRewardedState(RewardTransactionDTO evaluation, String rewardedInitiativeId, boolean expectedCap, long expectedCounterTrxNumber, double expectedCounterTotalAmount, double preCurrentTrxCounterTotalReward, boolean expectedCounterBudgetExhausted) {
        BigDecimal expectedCounterTotalReward = initiative2ExpectedReward.get(rewardedInitiativeId);
        assertRewardedState(evaluation, rewardedInitiativeId, expectedCounterTotalReward, expectedCap, expectedCounterTrxNumber, expectedCounterTotalAmount, expectedCounterTotalReward.doubleValue() + preCurrentTrxCounterTotalReward, expectedCounterBudgetExhausted);
    }

    @Override
    protected void assertRewardedState(RewardTransactionDTO evaluation, String rewardedInitiativeId, BigDecimal expectedReward, boolean expectedCap, long expectedCounterTrxNumber, double expectedCounterTotalAmount, double expectedCounterTotalReward, boolean expectedCounterBudgetExhausted) {
        super.assertRewardedState(evaluation, rewardedInitiativeId, expectedReward, expectedCap, expectedCounterTrxNumber, expectedCounterTotalAmount, expectedCounterTotalReward, expectedCounterBudgetExhausted);
        Assertions.assertNull(evaluation.getRewards().get(INITIATIVE_ID_EXPIRED));
    }

    private void assertRejectedInitiativesState(RewardTransactionDTO evaluation, Map<String, List<String>> expectedInitiativeRejectionReasons, List<String> expectedRejectionReasons) {
        if(evaluation.getRewards() != null && evaluation.getRewards().size()>0){
            Assertions.assertTrue(evaluation.getRewards().values().stream().noneMatch(r->BigDecimal.ZERO.compareTo(r.getAccruedReward()) !=0), "Expected rejection: %s".formatted(evaluation.getRewards()));
        }
        Assertions.assertEquals(expectedRejectionReasons, evaluation.getRejectionReasons());
        Assertions.assertEquals(expectedInitiativeRejectionReasons, evaluation.getInitiativeRejectionReasons());
        Assertions.assertEquals("REJECTED", evaluation.getStatus());
    }

    private TransactionDTO onboardTrxHpanAndIncreaseCounters(TransactionDTO trx, String... initiativeIds) {
        UserInitiativeCountersWrapper userInitiativeCountersWrapper = onboardTrxHPan(trx, initiativeIds);

        Arrays.stream(initiativeIds).forEach(id -> {
            InitiativeConfig initiativeConfig = Objects.requireNonNull(droolsRuleRepository.findById(id).block()).getInitiativeConfig();
            // the use case of the initiative INITIATIVE_ID_TRXCOUNT_BASED start with a count of 9 trx
            if (INITIATIVE_ID_TRXCOUNT_BASED.equals(id)) {
                userInitiativeCountersWrapper.getInitiatives().computeIfAbsent(
                        INITIATIVE_ID_TRXCOUNT_BASED,
                        i -> UserInitiativeCounters.builder(trx.getUserId(), INITIATIVE_ID_TRXCOUNT_BASED)
                                .trxNumber(TRX_NUMBER_MIN_NUMBER_INITIATIVE_ID_TRXCOUNT)
                                .build()
                );
            }
            updateInitiativeCounters(userInitiativeCountersWrapper
                            .getInitiatives().computeIfAbsent(id, x -> UserInitiativeCounters.builder(trx.getUserId(), id).build()),
                    trx, initiative2ExpectedReward.get(id), initiativeConfig);
        });

        return trx;
    }
    //endregion

    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp getErrorUseCaseIdPatternMatch
    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"correlationId\":\"CORRELATIONID([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();

    {
        Transaction2RewardTransactionMapper transaction2RewardTransactionMapper = new Transaction2RewardTransactionMapper();

        String useCaseJsonNotExpected = "{\"correlationId\":\"CORRELATIONID0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD] Unexpected JSON", useCaseJsonNotExpected, null)
        ));

        String jsonNotValid = "{\"correlationId\":\"CORRELATIONID1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD] Unexpected JSON", jsonNotValid, null)
        ));

        final String failingRuleEngineUserId = "FAILING_RULE_ENGINE";
        String failingRuleEngineUseCase = TestUtils.jsonSerializer(
                TransactionDTOFaker.mockInstanceBuilder(errorUseCases.size())
                        .userId(failingRuleEngineUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(ruleEngineServiceSpy).applyRules(Mockito.argThat(i -> failingRuleEngineUserId.equals(i.getUserId())), Mockito.any(), Mockito.any());
                    return failingRuleEngineUseCase;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD] An error occurred evaluating transaction", failingRuleEngineUseCase, failingRuleEngineUserId)
        ));

        final String failingCounterUpdateUserId = "FAILING_COUNTER_UPDATE";
        String failingCounterUpdate = TestUtils.jsonSerializer(
                TransactionDTOFaker.mockInstanceBuilder(errorUseCases.size())
                        .userId(failingCounterUpdateUserId)
                        .build()
        );
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new RuntimeException("DUMMYEXCEPTION")).when(userInitiativeCountersUpdateServiceSpy).update(Mockito.any(), Mockito.argThat(i -> failingCounterUpdateUserId.equals(i.getUserId())));
                    return failingCounterUpdate;
                },
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[REWARD] An error occurred evaluating transaction", failingCounterUpdate, failingCounterUpdateUserId)
        ));

        final String failingRewardPublishingUserId = "FAILING_REWARD_PUBLISHING";
        TransactionDTO failingRewardPublishing = TransactionDTOFaker.mockInstanceBuilder(errorUseCases.size())
                .userId(failingRewardPublishingUserId)
                .build();
        TransactionDTO expectedFailingRewardPublishing = failingRewardPublishing.toBuilder()
                .amount(CommonUtilities.centsToEuro(failingRewardPublishing.getAmount().longValue()))
                .amountCents(failingRewardPublishing.getAmount().longValue())
                .build();
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(false).when(rewardNotifierServiceSpy).notify(Mockito.argThat(i -> failingRewardPublishingUserId.equals(i.getUserId())));
                    createUserCounter(failingRewardPublishing);
                    return TestUtils.jsonSerializer(failingRewardPublishing);
                },
                errorMessage -> checkErrorMessageHeaders(topicRewardProcessorOutcome,"", errorMessage, "[REWARD] An error occurred while publishing the transaction evaluation result", TestUtils.jsonSerializer(trx2RejectedRewardNoInitiatives(transaction2RewardTransactionMapper, expectedFailingRewardPublishing)), failingRewardPublishingUserId, false, false)
        ));

        final String exceptionWhenRewardPublishUserId = "FAILING_REWARD_PUBLISHING_DUE_EXCEPTION";
        TransactionDTO exceptionWhenRewardPublish = TransactionDTOFaker.mockInstanceBuilder(errorUseCases.size())
                .userId(exceptionWhenRewardPublishUserId)
                .amount(BigDecimal.valueOf(88_00))
                .build();
        TransactionDTO expectedExceptionWhenRewardPublish = exceptionWhenRewardPublish.toBuilder()
                .amount(CommonUtilities.centsToEuro(exceptionWhenRewardPublish.getAmount().longValue()))
                .amountCents(exceptionWhenRewardPublish.getAmount().longValue())
                .build();
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new KafkaException()).when(rewardNotifierServiceSpy).notify(Mockito.argThat(i -> exceptionWhenRewardPublishUserId.equals(i.getUserId())));
                    createUserCounter(exceptionWhenRewardPublish);
                    return TestUtils.jsonSerializer(exceptionWhenRewardPublish);
                },
                errorMessage -> checkErrorMessageHeaders(topicRewardProcessorOutcome, "", errorMessage, "[REWARD] An error occurred while publishing the transaction evaluation result", TestUtils.jsonSerializer(trx2RejectedRewardNoInitiatives(transaction2RewardTransactionMapper, expectedExceptionWhenRewardPublish)), exceptionWhenRewardPublishUserId,false,false)
        ));
    }

    private static RewardTransactionDTO trx2RejectedRewardNoInitiatives(Transaction2RewardTransactionMapper transaction2RewardTransactionMapper, TransactionDTO failingRewardDueExceptionPublishing) {
        RewardTransactionDTO failingRewardPublishingDueExceptionNotifiedToError = transaction2RewardTransactionMapper.apply(failingRewardDueExceptionPublishing);
        failingRewardPublishingDueExceptionNotifiedToError.setTrxChargeDate(failingRewardPublishingDueExceptionNotifiedToError.getTrxDate());
        failingRewardPublishingDueExceptionNotifiedToError.setEffectiveAmount(failingRewardPublishingDueExceptionNotifiedToError.getAmount());
        failingRewardPublishingDueExceptionNotifiedToError.setOperationTypeTranscoded(OperationType.CHARGE);
        failingRewardPublishingDueExceptionNotifiedToError.setRejectionReasons(List.of(RewardConstants.TRX_REJECTION_REASON_NO_INITIATIVE));
        failingRewardPublishingDueExceptionNotifiedToError.setStatus(RewardConstants.REWARD_STATE_REJECTED);
        return failingRewardPublishingDueExceptionNotifiedToError;
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload, String expectedKey) {
        checkErrorMessageHeaders(topicRewardProcessorRequest, groupIdRewardProcessorRequest, errorMessage, errorDescription, expectedPayload, expectedKey);
    }
    //endregion
}