package it.gov.pagopa.reward.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

class TransactionProcessorRefundTest extends BaseTransactionProcessorTest {

    final LocalDateTime trxDateLocalDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(0, 0));
    final OffsetDateTime trxDate = OffsetDateTime.of(trxDateLocalDateTime, RewardConstants.ZONEID.getRules().getOffset(trxDateLocalDateTime));

    private final String initiativeId = "INITIATIVEID";
    private final String initiative2totalRefundId = "INITIATIVE2TOTALREFUND";
    private final String initiativeTrxMinId = "INITIATIVEMINCOUNTID";


    /**
     * Configuring just one initiative which will reward the 10% of the effective amount having temporal reward limits
     */
    private final InitiativeReward2BuildDTO initiative = InitiativeReward2BuildDTOFaker.mockInstanceBuilder(1, Collections.emptySet(), RewardValueDTO.class)
            .initiativeId(initiativeId)
            .initiativeName("NAME_"+initiativeId)
            .organizationId("ORGANIZATIONID_"+initiativeId)
            .general(InitiativeGeneralDTO.builder()
                    .beneficiaryBudget(BigDecimal.valueOf(100))
                    .startDate(LocalDate.MIN)
                    .build())
            .trxRule(InitiativeTrxConditions.builder()
                    .rewardLimits(List.of(
                            RewardLimitsDTO.builder()
                                    .frequency(RewardLimitsDTO.RewardLimitFrequency.DAILY)
                                    .rewardLimit(TestUtils.bigDecimalValue(2))
                                    .build(),
                            RewardLimitsDTO.builder()
                                    .frequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY)
                                    .rewardLimit(TestUtils.bigDecimalValue(10))
                                    .build(),
                            RewardLimitsDTO.builder()
                                    .frequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY)
                                    .rewardLimit(BigDecimal.valueOf(50))
                                    .build(),
                            RewardLimitsDTO.builder()
                                    .frequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY)
                                    .rewardLimit(BigDecimal.valueOf(100))
                                    .build()
                    ))
                    .build())
            .rewardRule(RewardValueDTO.builder()
                    .rewardValue(TestUtils.bigDecimalValue(10))
                    .build())
            .build();

    /**
     * initiative onboarded only to hpan of some use cases
     */
    private final InitiativeReward2BuildDTO initiative2totalRefund = InitiativeReward2BuildDTOFaker.mockInstanceBuilder(1, Collections.emptySet(), RewardValueDTO.class)
            .initiativeId(initiative2totalRefundId)
            .initiativeName("NAME_"+initiative2totalRefundId)
            .organizationId("ORGANIZATIONID_"+initiative2totalRefundId)
            .general(InitiativeGeneralDTO.builder()
                    .beneficiaryBudget(BigDecimal.valueOf(100))
                    .startDate(LocalDate.MIN)
                    .build())
            .trxRule(InitiativeTrxConditions.builder()
                    .threshold(ThresholdDTO.builder()
                            .from(BigDecimal.TEN)
                            .fromIncluded(true)
                            .build())
                    .build())
            .rewardRule(RewardValueDTO.builder()
                    .rewardValue(TestUtils.bigDecimalValue(10))
                    .build())
            .build();

    /**
     * initiative onboarded only to hpan of some use cases
     */
    private final InitiativeReward2BuildDTO initiativeTrxMin = InitiativeReward2BuildDTOFaker.mockInstanceBuilder(1, Collections.emptySet(), RewardValueDTO.class)
            .initiativeId(initiativeTrxMinId)
            .initiativeName("NAME_"+initiativeTrxMinId)
            .organizationId("ORGANIZATIONID_"+initiativeTrxMinId)
            .general(InitiativeGeneralDTO.builder()
                    .beneficiaryBudget(BigDecimal.valueOf(100))
                    .startDate(LocalDate.MIN)
                    .build())
            .trxRule(InitiativeTrxConditions.builder()
                    .threshold(ThresholdDTO.builder()
                            .from(BigDecimal.valueOf(100))
                            .fromIncluded(true)
                            .build())
                    .trxCount(TrxCountDTO.builder()
                            .from(5L)
                            .fromIncluded(true)
                            .build())
                    .build())
            .rewardRule(RewardValueDTO.builder()
                    .rewardValue(TestUtils.bigDecimalValue(10))
                    .build())
            .build();

    @Test
    void test() throws JsonProcessingException {
        int totalRefundedTrxs = Math.max(10, totalRefundUseCases.size()); // it's more readable if the result is a multiple of 10
        int partialRefundedTrxs = Math.max(10, partialRefundUseCases.size());
        long maxWaitingMs = 30000;

        publishRewardRules(List.of(initiative, initiative2totalRefund, initiativeTrxMin));

        List<TransactionDTO> trxs = new ArrayList<>(IntStream.range(0, totalRefundedTrxs).mapToObj(this::buildTotalRefundRequests).flatMap(List::stream).toList());
        trxs.addAll(IntStream.range(totalRefundedTrxs, totalRefundedTrxs + partialRefundedTrxs).mapToObj(this::buildPartialRefundRequests).flatMap(List::stream).toList());

        trxs.forEach(t -> onboardHpan(t.getHpan(), trxDateLocalDateTime, trxDateLocalDateTime.plusDays(10), initiativeId));

        long timePublishOnboardingStart = System.currentTimeMillis();
        trxs.forEach(i -> publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, i.getUserId(), i));
        publishIntoEmbeddedKafka(topicRewardProcessorRequest, List.of(new RecordHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = consumeMessages(topicRewardProcessorOutcome, trxs.size(), maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(trxs.size(), payloadConsumed.size());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            RewardTransactionDTO payload = objectMapper.readValue(p.value(), RewardTransactionDTO.class);
            checkResponse(payload, totalRefundedTrxs);
            Assertions.assertEquals(payload.getUserId(), p.key());
        }

        System.out.printf("""
                        ************************
                        Time spent to send %d trx messages: %d millis
                        Time spent to consume reward responses: %d millis
                        ************************
                        Test Completed in %d millis
                        ************************
                        """,
                trxs.size(),
                timePublishingOnboardingRequest,
                timeConsumerResponseEnd,
                timeEnd - timePublishOnboardingStart
        );

//        checkOffsets(trxs.size()+1, trxs.size()); heavy test removed because already performed in TransactionProcessorTest
    }

    private List<TransactionDTO> buildTotalRefundRequests(int bias) {
        return totalRefundUseCases.get(bias % totalRefundUseCases.size()).useCaseBuilder().apply(bias);
    }

    private List<TransactionDTO> buildPartialRefundRequests(int bias) {
        return partialRefundUseCases.get(bias % partialRefundUseCases.size()).useCaseBuilder().apply(bias);
    }

    private void checkResponse(RewardTransactionDTO rewardedTrx, int completeRefundedTrxs) {
        String hpan = rewardedTrx.getHpan();
        int bias = Integer.parseInt(hpan.substring(4));
        boolean isTotalRefundUseCase = bias < completeRefundedTrxs;

        try {
            if (rewardedTrx.getOperationType().equals("00")) {
                checkChargeOp(rewardedTrx, isTotalRefundUseCase, bias);
            } else {
                checkRefundOp(rewardedTrx, isTotalRefundUseCase, bias);
            }
            checkUserInitiativeCounter(rewardedTrx, isTotalRefundUseCase, bias);
        } catch (Throwable e){
            System.err.printf("Failed use case %s %d on user %s opType %s and IdTrxAcquirer %s%n",
                    isTotalRefundUseCase ? "TOTAL_REFUND" : "PARTIAL_REFUND",
                    Math.floorMod(isTotalRefundUseCase? bias: bias-completeRefundedTrxs-1, (isTotalRefundUseCase ? totalRefundUseCases.size(): partialRefundUseCases.size())),
                    rewardedTrx.getUserId(),
                    rewardedTrx.getOperationTypeTranscoded(),
                    rewardedTrx.getIdTrxAcquirer());
            throw e;
        }
    }

    private void checkChargeOp(RewardTransactionDTO rewardedTrx, boolean isTotalRefundUseCase, int bias) {
        Assertions.assertEquals(OperationType.CHARGE, rewardedTrx.getOperationTypeTranscoded());
        Assertions.assertEquals(Collections.emptyList(), rewardedTrx.getRejectionReasons());

        List<RefundUseCase> refundUseCases = isTotalRefundUseCase ? totalRefundUseCases : partialRefundUseCases;
        RefundUseCase useCase = refundUseCases.get(bias % refundUseCases.size());

        List<String> expectedChargeInitiativeRejectionReasons = useCase.expectedChargeInitiativeRejectionReasons();
        boolean expectedRewarded = CollectionUtils.isEmpty(expectedChargeInitiativeRejectionReasons);
        String expectedStatus = expectedRewarded ? "REWARDED" : "REJECTED";
        Assertions.assertEquals(expectedRewarded
                        ? null
                        : expectedChargeInitiativeRejectionReasons,
                rewardedTrx.getInitiativeRejectionReasons().get(useCase.initiativeId2Test()));
        Assertions.assertEquals(
                expectedStatus,
                rewardedTrx.getStatus(), "Trx not in %s state: %s; %s".formatted(expectedStatus, rewardedTrx.getInitiativeRejectionReasons(), rewardedTrx.getRewards()));
        useCase.chargeRewardVerifier().accept(rewardedTrx);
    }

    private void checkRefundOp(RewardTransactionDTO rewardedTrx, boolean isTotalRefundUseCase, int bias) {
        Assertions.assertEquals(OperationType.REFUND, rewardedTrx.getOperationTypeTranscoded());

        List<RefundUseCase> refundUseCases = isTotalRefundUseCase ? totalRefundUseCases : partialRefundUseCases;
        refundUseCases.get(bias % refundUseCases.size()).refundVerifier().accept(rewardedTrx);
    }

    private void checkUserInitiativeCounter(RewardTransactionDTO rewardedTrx, boolean isTotalRefundUseCase, int bias) {
        final UserInitiativeCounters userInitiativeCounters = userInitiativeCountersRepository.findById(rewardedTrx.getUserId()).block();
        Assertions.assertNotNull(userInitiativeCounters);

        List<RefundUseCase> refundUseCases = isTotalRefundUseCase ? totalRefundUseCases : partialRefundUseCases;
        final int useCase = bias % refundUseCases.size();
        RefundUseCase refundUseCase = refundUseCases.get(useCase);

        InitiativeCounters initiativeCounters = userInitiativeCounters.getInitiatives().get(refundUseCase.initiativeId2Test);
        Assertions.assertNotNull(initiativeCounters);

        refundUseCase.expectedInitiativeCounterSupplier().get().forEach(initiativeCounter ->
                Assertions.assertEquals(
                        initiativeCounter,
                        userInitiativeCounters.getInitiatives().get(initiativeCounter.getInitiativeId()),
                        "Failed user initiative counter (%s) compare on user %s (case %s %d)"
                                .formatted(initiativeCounter.getInitiativeId(),
                                        rewardedTrx.getUserId(),
                                        isTotalRefundUseCase ? "TOTAL_REFUND" : "PARTIAL_REFUND",
                                        useCase)
                ));
    }

    private record RefundUseCase(
            String initiativeId2Test,
            Function<Integer, List<TransactionDTO>> useCaseBuilder,
            Consumer<RewardTransactionDTO> chargeRewardVerifier,
            Consumer<RewardTransactionDTO> refundVerifier,
            List<String> expectedChargeInitiativeRejectionReasons,
            Supplier<List<InitiativeCounters>> expectedInitiativeCounterSupplier) {
    }

    /**
     * it will build a transaction having an amount of 10
     */
    private static TransactionDTO buildChargeTrx(OffsetDateTime trxDate, Integer i) {
        final TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
        trx.setTrxDate(trxDate);
        trx.setAmount(TestUtils.bigDecimalValue(10));
        return trx;
    }

    private static TransactionDTO buildRefundTrx(Integer i, TransactionDTO trx) {
        final TransactionDTO partialRefund = TransactionDTOFaker.mockInstance(i);
        partialRefund.setOperationType("01");
        partialRefund.setTrxDate(trx.getTrxDate().plusDays(i + 1));
        return partialRefund;
    }

    //region total refund useCases
    /**
     * Each trx is built using {@link #buildChargeTrx(OffsetDateTime, Integer)} method and its hpan is next onboarded in order to activate the {@link #initiative}
     */
    private final List<RefundUseCase> totalRefundUseCases = new ArrayList<>();

    {
        // 0: Base use case: Sending a charge trx of 10 amount, expecting a reward of 1, for a user never meet (no user counter); then totally refunding it
        totalRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());
                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false, 1L, 10, 1, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-1), false, 0L, 0, 0, false, true, true),
                Collections.emptyList(),
                () -> List.of(buildSimpleFullTemporalInitiativeCounter(0L, 0, 0, trxDate)))
        );

        // 1: Counter already initiated: as use case 0, but the user has already a counter, thus checking that after the refund it has been restored
        final InitiativeCounters useCaseCounterAlreadyInitiated_initialStateOfCounters =
                buildSimpleFullTemporalInitiativeCounter(1L, 10, 1, trxDate);
        totalRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());

                    saveUserInitiativeCounter(trx, useCaseCounterAlreadyInitiated_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false, 2L, 20, 2, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-1), false, 1L, 10, 1, false, true, true),
                Collections.emptyList(),
                () -> List.of(useCaseCounterAlreadyInitiated_initialStateOfCounters)
        ));

        // 2: Charge reward limited initiative: as use case 1, but the user's counter now triggers the weekly reward limit leading to a reward of 0.9
        final InitiativeCounters useCaseChargeRewardLimited_initialStateOfCounters =
                // counter configured on the same week of the current trx: thus the daily limit is not involved
                buildSimpleFullTemporalInitiativeCounter(1L, 10, 9.1, trxDate.plusDays(1));
        totalRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());

                    saveUserInitiativeCounter(trx, useCaseChargeRewardLimited_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true, 2L, 20, 10, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.9), false, 1L, 10, 9.1, false, true, true),
                Collections.emptyList(),
                () -> {
                    useCaseChargeRewardLimited_initialStateOfCounters.getDailyCounters().put("2022-01-01", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    return List.of(useCaseChargeRewardLimited_initialStateOfCounters);
                }
        ));

        // 3: Charge exhausting initiative budget: as use case 1, but the user's counter now triggers the exhaustion of the beneficiary initiative budget; after refund the status of the initiative should be restored (not more exhausted)
        final InitiativeCounters useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters =
                // placing a rewarded day of 99.9 to the previous year in order to avoid reward limits on current
                buildSimpleFullTemporalInitiativeCounter(1L, 1000, 99.9, trxDate.minusYears(1));
        totalRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());

                    saveUserInitiativeCounter(trx, useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.1), true, 2L, 1010, 100, true),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.1), false, 1L, 1000, 99.9, false, true, true),
                Collections.emptyList(),
                () -> {
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.getDailyCounters().put("2022-01-01", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.getWeeklyCounters().put("2022-01-0", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.getMonthlyCounters().put("2022-01", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.getYearlyCounters().put("2022", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    return List.of(useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters);
                }
        ));

        // 4: Charge reward limited initiative: as use case 1, but the charge itself is already capped to 0
        final InitiativeCounters useCaseChargeRewardLimitedCapped_initialStateOfCounters =
                // counter configured on the same week of the current trx: thus the daily limit is not involved
                buildSimpleFullTemporalInitiativeCounter(1L, 10, 20, trxDate.plusDays(1));
        totalRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());

                    saveUserInitiativeCounter(trx, useCaseChargeRewardLimitedCapped_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRejectedState(chargeReward, initiativeId, List.of("TRX_RULE_REWARDLIMITS_WEEKLY_FAIL"), 1L, 10, 20, false, false, false),
                refundReward -> assertRejectedState(refundReward, initiativeId, List.of("TRX_RULE_REWARDLIMITS_WEEKLY_FAIL"), 0L, 10, 20, false, true, true),
                List.of("TRX_RULE_REWARDLIMITS_WEEKLY_FAIL"),
                () -> List.of(useCaseChargeRewardLimitedCapped_initialStateOfCounters)
        ));

        // 5: trxMin case never reached
        final InitiativeCounters useCaseTrxCountMinNeverReached_initialStateOfCounters = buildSimpleFullTemporalInitiativeCounter(initiativeTrxMinId, 3L, 10, 8, trxDate.plusDays(11));
        totalRefundUseCases.add(new RefundUseCase(
                initiativeTrxMinId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate.plusDays(11), i);
                    trx.setAmount(BigDecimal.valueOf(100));

                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());

                    onboardHpan(trx.getHpan(), trx.getTrxDate().toLocalDateTime(), null, initiativeTrxMinId);
                    saveUserInitiativeCounter(trx, useCaseTrxCountMinNeverReached_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 4L, 10, 8, false, false, false),
                refundReward -> assertRejectedState(refundReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 3L, 10, 8, false, true, true),
                List.of("TRX_RULE_TRXCOUNT_FAIL"),
                () -> List.of(useCaseTrxCountMinNeverReached_initialStateOfCounters)
        ));

        // 6: trxMin reached then refunded
        final InitiativeCounters useCaseTrxCountMinReached_initialStateOfCounters = buildSimpleFullTemporalInitiativeCounter(initiativeTrxMinId, 4L, 10, 8, trxDate.plusDays(11));
        totalRefundUseCases.add(new RefundUseCase(
                initiativeTrxMinId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate.plusDays(11), i);
                    trx.setAmount(BigDecimal.valueOf(100));

                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());

                    onboardHpan(trx.getHpan(), trx.getTrxDate().toLocalDateTime(), null, initiativeTrxMinId);
                    saveUserInitiativeCounter(trx, useCaseTrxCountMinReached_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeTrxMinId, TestUtils.bigDecimalValue(10), false, 5L, 110, 18,  false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeTrxMinId, BigDecimal.valueOf(-10), false, 4L, 10, 8, false, true, true),
                Collections.emptyList(),
                () -> List.of(useCaseTrxCountMinReached_initialStateOfCounters)
        ));

    }
    //endregion

    //region partial refund useCases
    /**
     * Each trx is built using {@link #buildChargeTrx(OffsetDateTime, Integer)} method and its hpan is next onboarded in order to activate the {@link #initiative}
     */
    private final List<RefundUseCase> partialRefundUseCases = new ArrayList<>();

    {
        // 0: Base use case: Sending a charge trx of 10 amount, expecting a reward of 1, for a user never meet (no user counter); then refunding 1 thus expecting a reward of -0.1
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.ONE);
                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false, 1L, 10, 1, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.1), true, 1L, 9, 0.9, false, true, false),
                Collections.emptyList(),
                () -> List.of(buildSimpleFullTemporalInitiativeCounter(1L, 9, 0.9, trxDate))
        ));

        // 1: Counter already initiated: as use case 0, but the user has already a counter, thus checking that after the partial refund its amount and reward are updated (the trx number is still +1)
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.ONE);

                    saveUserInitiativeCounter(trx,
                            buildSimpleFullTemporalInitiativeCounter(1L, 10, 1, trxDate)
                    );

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false, 2L, 20, 2, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.1), true, 2L, 19, 1.9, false, true, false),
                Collections.emptyList(),
                () -> List.of(buildSimpleFullTemporalInitiativeCounter(2L, 19, 1.9, trxDate))
        ));

        // 2: Charge reward limited initiative and partially refunded leading to a reward equal to limit:
        //       as use case 1, but the user's counter now triggers the weekly reward limit leading to a reward of 0.9;
        //       the partial reward will lead to a reward of 0 because equal to the reward limit;
        //       thus checking that after the partial refund its amount is updated (the trx number and total reward will not change)
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.ONE);

                    saveUserInitiativeCounter(trx, buildSimpleFullTemporalInitiativeCounter(1L, 10, 9.1, trxDate.plusDays(1)));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true, 2L, 20, 10, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.ZERO, true, 2L, 19, 10, false, true, false),
                Collections.emptyList(),
                () -> {
                    InitiativeCounters expectedCounter = buildSimpleFullTemporalInitiativeCounter(2L, 19, 10, trxDate.plusDays(1));
                    expectedCounter.getDailyCounters().put("2022-01-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(9)).totalReward(TestUtils.bigDecimalValue(0.9)).build());
                    expectedCounter.getDailyCounters().get("2022-01-02").setTrxNumber(1L);
                    expectedCounter.getDailyCounters().get("2022-01-02").setTotalAmount(TestUtils.bigDecimalValue(10));
                    expectedCounter.getDailyCounters().get("2022-01-02").setTotalReward(TestUtils.bigDecimalValue(9.1));
                    return List.of(expectedCounter);
                }
        ));

        // 3: Charge reward limited initiative and partially refunded leading to a reward under the limit: as use case 2, but now the refun is under the limit leading to a reward of -0.1, thus checking that counters are updated (both totalAmount and totalReward, trxNumber still +1)
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.valueOf(2));

                    saveUserInitiativeCounter(trx, buildSimpleFullTemporalInitiativeCounter(1L, 10, 9.1, trxDate.plusDays(1)));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true, 2L, 20, 10, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.1), true, 2L, 18, 9.9, false, true, false),
                Collections.emptyList(),
                () -> {
                    InitiativeCounters expectedCounter = buildSimpleFullTemporalInitiativeCounter(2L, 18, 9.9, trxDate.plusDays(1));
                    expectedCounter.getDailyCounters().put("2022-01-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(8)).totalReward(TestUtils.bigDecimalValue(0.8)).build());
                    expectedCounter.getDailyCounters().get("2022-01-02").setTrxNumber(1L);
                    expectedCounter.getDailyCounters().get("2022-01-02").setTotalAmount(TestUtils.bigDecimalValue(10));
                    expectedCounter.getDailyCounters().get("2022-01-02").setTotalReward(TestUtils.bigDecimalValue(9.1));
                    return List.of(expectedCounter);
                }
        ));

        // 4: Charge exhausting initiative budget and partially refunded leading to a reward equal to budget limit: as use case 1, but the user's counter now triggers the exhaustion of the beneficiary initiative budget; after refund the status of the initiative should be still exhausted
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.ONE);

                    saveUserInitiativeCounter(trx,
                            // placing a rewarded day of 99.9 to the previous year in order to avoid reward limits on current
                            buildSimpleFullTemporalInitiativeCounter(1L, 1000, 99.1, trxDate.minusYears(1)));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true, 2L, 1010, 100, true),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.ZERO, true, 2L, 1009, 100, true, true, false),
                Collections.emptyList(),
                () -> {
                    InitiativeCounters expectedCounter = buildSimpleFullTemporalInitiativeCounter(1L, 9, 0.9, trxDate);
                    expectedCounter.setTrxNumber(2L);
                    expectedCounter.setTotalAmount(TestUtils.bigDecimalValue(1009));
                    expectedCounter.setTotalReward(TestUtils.bigDecimalValue(100));
                    expectedCounter.setExhaustedBudget(true);
                    expectedCounter.getDailyCounters().put("2021-01-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(1000)).totalReward(TestUtils.bigDecimalValue(99.1)).build());
                    expectedCounter.getWeeklyCounters().put("2021-01-0", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(1000)).totalReward(TestUtils.bigDecimalValue(99.1)).build());
                    expectedCounter.getMonthlyCounters().put("2021-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(1000)).totalReward(TestUtils.bigDecimalValue(99.1)).build());
                    expectedCounter.getYearlyCounters().put("2021", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(1000)).totalReward(TestUtils.bigDecimalValue(99.1)).build());
                    return List.of(expectedCounter);
                }
        ));

        // 5: Charge exhausting initiative budget and partially refunded leading to a reward under budget limit: as use case 4, but after the refund the status of the initiative should be restored (not more exhausted)
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.valueOf(2));

                    saveUserInitiativeCounter(trx,
                            // placing a rewarded day of 99.9 to the previous year in order to avoid reward limits on current
                            buildSimpleFullTemporalInitiativeCounter(1L, 1000, 99.1, trxDate.minusYears(1)));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true, 2L, 1010, 100, true),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, TestUtils.bigDecimalValue(-0.1), true, 2L, 1008, 99.9, false, true, false),
                Collections.emptyList(),
                () -> {
                    InitiativeCounters expectedCounter = buildSimpleFullTemporalInitiativeCounter(1L, 8, 0.8, trxDate);
                    expectedCounter.setTrxNumber(2L);
                    expectedCounter.setTotalAmount(TestUtils.bigDecimalValue(1008));
                    expectedCounter.setTotalReward(TestUtils.bigDecimalValue(99.9));
                    expectedCounter.setExhaustedBudget(false);
                    expectedCounter.getDailyCounters().put("2021-01-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(1000)).totalReward(TestUtils.bigDecimalValue(99.1)).build());
                    expectedCounter.getWeeklyCounters().put("2021-01-0", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(1000)).totalReward(TestUtils.bigDecimalValue(99.1)).build());
                    expectedCounter.getMonthlyCounters().put("2021-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(1000)).totalReward(TestUtils.bigDecimalValue(99.1)).build());
                    expectedCounter.getYearlyCounters().put("2021", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(1000)).totalReward(TestUtils.bigDecimalValue(99.1)).build());
                    return List.of(expectedCounter);
                }
        ));

        // 6: partial with reward not more present: as use case 0, but the charge will be rewarded by a new initiative: this initiative other than the negative reward, will be reported between rejected initiatives
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.ONE);

                    onboardHpan(trx.getHpan(), trx.getTrxDate().toLocalDateTime(), null, initiative2totalRefund.getInitiativeId());

                    return List.of(trx, partialRefund);
                },
                chargeReward -> {
                    assertRewardedState(chargeReward, 2, initiativeId, TestUtils.bigDecimalValue(1), false, 1L, 10, 1, false, false, false);
                    assertRewardedState(chargeReward, 2, initiative2totalRefundId, TestUtils.bigDecimalValue(1), false, 1L, 10, 1, false, false, false);
                },
                refundReward -> {
                    Assertions.assertEquals(Map.of(initiative2totalRefundId, List.of("TRX_RULE_THRESHOLD_FAIL")), refundReward.getInitiativeRejectionReasons());
                    refundReward.setInitiativeRejectionReasons(Collections.emptyMap());

                    assertRewardedState(refundReward, 2, initiativeId, BigDecimal.valueOf(-0.1), true, 1L, 9, 0.9, false, true, false);
                    assertRewardedState(refundReward, 2, initiative2totalRefundId, BigDecimal.valueOf(-1), false, 0L, 0, 0.0, false, true, true);
                },
                Collections.emptyList(),
                () -> {
                    InitiativeCounters baseInitiativeCounter = buildSimpleFullTemporalInitiativeCounter(1L, 9, 0.9, trxDate);
                    InitiativeCounters initiative2totalTefundIdCounter = InitiativeCounters.builder().initiativeId(initiative2totalRefundId).build();
                    return List.of(baseInitiativeCounter, initiative2totalTefundIdCounter);
                }
        ));

        // 7: partial refunds until total: as use case 0, but there will be more than 1 refund until complete amount reverted
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);

                    final TransactionDTO partialRefund1 = buildRefundTrx(i, trx);
                    partialRefund1.setAmount(BigDecimal.ONE);
                    partialRefund1.setIdTrxAcquirer("REFUND1");

                    final TransactionDTO partialRefund2 = buildRefundTrx(i, trx);
                    partialRefund2.setAmount(BigDecimal.valueOf(7));
                    partialRefund2.setIdTrxAcquirer("REFUND2");

                    final TransactionDTO partialRefund3 = buildRefundTrx(i, trx);
                    partialRefund3.setAmount(BigDecimal.valueOf(2));
                    partialRefund3.setIdTrxAcquirer("REFUND3");

                    return List.of(trx, partialRefund1, partialRefund2, partialRefund3);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false, 1L, 10, 1, false),
                refundReward -> {
                    if ("REFUND1".equals(refundReward.getIdTrxAcquirer())) {
                        assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.1), true, 1L, 9, 0.9, false, true, false);
                    } else if ("REFUND2".equals(refundReward.getIdTrxAcquirer())) {
                        assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.7), true, 1L, 2, 0.2, false, true, false);
                    } else if ("REFUND3".equals(refundReward.getIdTrxAcquirer())) {
                        assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.2), false, 0L, 0, 0, false, true, true);
                    }
                },
                Collections.emptyList(),
                () -> List.of(buildSimpleFullTemporalInitiativeCounter(0L, 0, 0, trxDate))
        ));

        // 8: Partial refund of charge transaction capped to 0
        final InitiativeCounters useCaseChargeRewardLimitedCapped_initialStateOfCounters =
                // counter configured on the same week of the current trx: thus the daily limit is not involved
                buildSimpleFullTemporalInitiativeCounter(1L, 10, 20, trxDate.plusDays(1));
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(BigDecimal.ONE);

                    saveUserInitiativeCounter(trx, useCaseChargeRewardLimitedCapped_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRejectedState(chargeReward, initiativeId, List.of("TRX_RULE_REWARDLIMITS_WEEKLY_FAIL"), 1L, 10, 20, false, false, false),
                refundReward -> assertRejectedState(refundReward, initiativeId, List.of("TRX_RULE_REWARDLIMITS_WEEKLY_FAIL"), 1L, 10, 20, false, true, false),
                List.of("TRX_RULE_REWARDLIMITS_WEEKLY_FAIL"),
                () -> List.of(useCaseChargeRewardLimitedCapped_initialStateOfCounters)
        ));

        // 9: trxMin case never reached
        partialRefundUseCases.add(new RefundUseCase(
                initiativeTrxMinId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate.plusDays(11), i);
                    trx.setAmount(BigDecimal.valueOf(101));

                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.ONE);

                    onboardHpan(trx.getHpan(), trx.getTrxDate().toLocalDateTime(), null, initiativeTrxMinId);
                    saveUserInitiativeCounter(trx, buildSimpleInitiativeCounter(initiativeTrxMinId, 3L, 10, 8));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 4L, 10, 8, false, false, false),
                refundReward -> assertRejectedState(refundReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 4L, 10, 8, false, true, false),
                List.of("TRX_RULE_TRXCOUNT_FAIL"),
                () -> List.of(buildSimpleInitiativeCounter(initiativeTrxMinId, 4L, 10, 8))
        ));

        // 10: trxMin reached then refunded
        partialRefundUseCases.add(new RefundUseCase(
                initiativeTrxMinId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate.plusDays(11), i);
                    trx.setAmount(BigDecimal.valueOf(101));

                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.ONE);

                    onboardHpan(trx.getHpan(), trx.getTrxDate().toLocalDateTime(), null, initiativeTrxMinId);
                    saveUserInitiativeCounter(trx, buildSimpleInitiativeCounter(initiativeTrxMinId, 4L, 10, 8));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeTrxMinId, TestUtils.bigDecimalValue(10.1), false, 5L, 111, 18.1,  false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeTrxMinId, BigDecimal.valueOf(-0.1), true, 5L, 110, 18, false, true, false),
                Collections.emptyList(),
                () -> List.of(buildSimpleInitiativeCounter(initiativeTrxMinId, 5L, 110, 18))
        ));
    }

    private static InitiativeCounters buildSimpleInitiativeCounter(String initiativeId, long trxNumber, double totalAmount, double totalReward) {
        return InitiativeCounters.builder()
                .initiativeId(initiativeId)
                .trxNumber(trxNumber)
                .totalAmount(TestUtils.bigDecimalValue(totalAmount))
                .totalReward(TestUtils.bigDecimalValue(totalReward)).build();
    }

    private InitiativeCounters buildSimpleFullTemporalInitiativeCounter(long trxNumber, double totalAmount, double totalReward, OffsetDateTime trxDate) {
        return buildSimpleFullTemporalInitiativeCounter(initiativeId, trxNumber, totalAmount, totalReward, trxDate);
    }

    private InitiativeCounters buildSimpleFullTemporalInitiativeCounter(String initiativeId, long trxNumber, double totalAmount, double totalReward, OffsetDateTime trxDate) {
        BigDecimal totalAmountBigDecimal = TestUtils.bigDecimalValue(totalAmount);
        BigDecimal totalRewardBigDecimal = TestUtils.bigDecimalValue(totalReward);
        return InitiativeCounters.builder()
                .initiativeId(initiativeId)
                .trxNumber(trxNumber)
                .totalAmount(totalAmountBigDecimal)
                .totalReward(totalRewardBigDecimal)
                .dailyCounters(new HashMap<>(Map.of(dayFormatter.format(trxDate), Counters.builder().trxNumber(trxNumber).totalAmount(totalAmountBigDecimal).totalReward(totalRewardBigDecimal).build())))
                .weeklyCounters(new HashMap<>(Map.of(weekFormatter.format(trxDate), Counters.builder().trxNumber(trxNumber).totalAmount(totalAmountBigDecimal).totalReward(totalRewardBigDecimal).build())))
                .monthlyCounters(new HashMap<>(Map.of(monthlyFormatter.format(trxDate), Counters.builder().trxNumber(trxNumber).totalAmount(totalAmountBigDecimal).totalReward(totalRewardBigDecimal).build())))
                .yearlyCounters(new HashMap<>(Map.of(yearFormatter.format(trxDate), Counters.builder().trxNumber(trxNumber).totalAmount(totalAmountBigDecimal).totalReward(totalRewardBigDecimal).build())))
                .build();
    }
    //endregion
}
