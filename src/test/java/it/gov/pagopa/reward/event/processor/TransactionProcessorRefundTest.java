package it.gov.pagopa.reward.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.TransactionDTO;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

    private final String initiativeId = "INITIATIVEID";

    @Test
    void test() throws JsonProcessingException {
        int totalRefundedTrxs = Math.max(10, totalRefundUseCases.size());
        int partialRefundedTrxs = Math.max(10, partialRefundUseCases.size());
        long maxWaitingMs = 30000;

        publishRewardRules(List.of(InitiativeReward2BuildDTOFaker.mockInstanceBuilder(1, Collections.emptySet(), RewardValueDTO.class)
                .initiativeId(initiativeId)
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
                .build()
        ));

        List<TransactionDTO> trxs = new ArrayList<>(IntStream.range(0, totalRefundedTrxs).mapToObj(this::buildTotalRefundRequests).flatMap(List::stream).toList());
        trxs.addAll(IntStream.range(totalRefundedTrxs, totalRefundedTrxs+partialRefundedTrxs).mapToObj(this::buildPartialRefundRequests).flatMap(List::stream).toList());

        trxs.forEach(t -> onboardHpan(t.getHpan(), t.getTrxDate().toLocalDateTime(), null, initiativeId));

        long timePublishOnboardingStart = System.currentTimeMillis();
        trxs.forEach(i -> publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, i.getUserId(), i));
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

//        checkOffsets(trxs.size(), trxs.size()); heavy test removed because already performed in TransactionProcessorTest
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

        if (rewardedTrx.getOperationType().equals("00")) {
            checkChargeOp(rewardedTrx, isTotalRefundUseCase, bias);
        } else {
            checkRefundOp(rewardedTrx, isTotalRefundUseCase, bias);
        }
        checkUserInitiativeCounter(rewardedTrx, isTotalRefundUseCase, bias);
    }

    private void checkChargeOp(RewardTransactionDTO rewardedTrx, boolean isTotalRefundUseCase, int bias) {
        Assertions.assertEquals(OperationType.CHARGE, rewardedTrx.getOperationTypeTranscoded());
        Assertions.assertEquals(Collections.emptyList(), rewardedTrx.getRejectionReasons());
        Assertions.assertEquals(Collections.emptyMap(), rewardedTrx.getInitiativeRejectionReasons());
        Assertions.assertEquals("REWARDED", rewardedTrx.getStatus());

        List<RefundUseCase> refundUseCases = isTotalRefundUseCase ? totalRefundUseCases : partialRefundUseCases;
        refundUseCases.get(bias % refundUseCases.size()).chargeRewardVerifier().accept(rewardedTrx);
    }

    private void checkRefundOp(RewardTransactionDTO rewardedTrx, boolean isTotalRefundUseCase, int bias) {
        Assertions.assertEquals(OperationType.REFUND, rewardedTrx.getOperationTypeTranscoded());
        Assertions.assertEquals("REWARDED", rewardedTrx.getStatus());

        List<RefundUseCase> refundUseCases = isTotalRefundUseCase ? totalRefundUseCases : partialRefundUseCases;
        refundUseCases.get(bias % refundUseCases.size()).refundVerifier().accept(rewardedTrx);
    }

    private void checkUserInitiativeCounter(RewardTransactionDTO rewardedTrx, boolean isTotalRefundUseCase, int bias) {
        final UserInitiativeCounters userInitiativeCounters = userInitiativeCountersRepository.findById(rewardedTrx.getUserId()).block();
        Assertions.assertNotNull(userInitiativeCounters);
        InitiativeCounters initiativeCounters = userInitiativeCounters.getInitiatives().get(initiativeId);
        Assertions.assertNotNull(initiativeCounters);

        List<RefundUseCase> refundUseCases = isTotalRefundUseCase ? totalRefundUseCases : partialRefundUseCases;
        final int useCase = bias % refundUseCases.size();
        Assertions.assertEquals(
                refundUseCases.get(useCase).expectedInitiativeCounterSupplier().get(),
                initiativeCounters,
                "Failed user initiative counter compare on user %s (case %s %d)"
                        .formatted(rewardedTrx.getUserId(),
                                isTotalRefundUseCase ? "TOTAL_REFUND" : "PARTIAL_REFUND",
                                useCase)
        );
    }

    private record RefundUseCase(
            Function<Integer, List<TransactionDTO>> useCaseBuilder,
            Consumer<RewardTransactionDTO> chargeRewardVerifier,
            Consumer<RewardTransactionDTO> refundVerifier,
            Supplier<InitiativeCounters> expectedInitiativeCounterSupplier) {
    }

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
    private final List<RefundUseCase> totalRefundUseCases = new ArrayList<>();

    {
        final LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(0, 0));
        final OffsetDateTime trxDate = OffsetDateTime.of(localDateTime, RewardConstants.ZONEID.getRules().getOffset(localDateTime));

        // 0: Base use case
        totalRefundUseCases.add(new RefundUseCase(
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());
                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false,1L, 10, 1, false),
                refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.valueOf(-1), false, 0L, 0, 0, false),
                () -> buildSimpleFullTemporalInitiativeCounter(0L, 0, 0, trxDate))
        );

        // 1: Counter already initiated
        final InitiativeCounters useCaseCounterAlreadyInitiated_initialStateOfCounters =
                buildSimpleFullTemporalInitiativeCounter(1L, 10, 1, trxDate);
        totalRefundUseCases.add(new RefundUseCase(
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());

                    saveUserInitiativeCounter(trx, useCaseCounterAlreadyInitiated_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false,2L, 20, 2, false),
                refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.valueOf(-1), false, 1L, 10, 1, false),
                () -> useCaseCounterAlreadyInitiated_initialStateOfCounters
        ));

        // 2: Charge reward limited initiative
        final InitiativeCounters useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters =
                buildSimpleFullTemporalInitiativeCounter(1L, 10, 9.1, trxDate.plusDays(1));
        totalRefundUseCases.add(new RefundUseCase(
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());

                    saveUserInitiativeCounter(trx, useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true,2L, 20, 10, false),
                refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.valueOf(-0.9), false, 1L, 10, 9.1, false),
                () -> {
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.getDailyCounters().put("2022-01-01", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    return useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters;
                }
        ));

        // 3: Charge exhausting initiative budget TODO
    }
    //endregion


    //region partial refund useCases
    private final List<RefundUseCase> partialRefundUseCases = new ArrayList<>();

    {
        final LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(0, 0));
        final OffsetDateTime trxDate = OffsetDateTime.of(localDateTime, RewardConstants.ZONEID.getRules().getOffset(localDateTime));

        // 0: Base use case
        partialRefundUseCases.add(new RefundUseCase(
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.ONE);
                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false,1L, 10, 1, false),
                refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.valueOf(-0.1), true, 1L, 9, 0.9, false),
                () -> buildSimpleFullTemporalInitiativeCounter(1L, 9, 0.9, trxDate)
        ));

        // 1: Counter already initiated
        partialRefundUseCases.add(new RefundUseCase(
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.ONE);

                    saveUserInitiativeCounter(trx,
                            buildSimpleFullTemporalInitiativeCounter(1L, 10, 1, trxDate)
                    );

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false,2L, 20, 2, false),
                refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.valueOf(-0.1), true, 2L, 19, 1.9, false),
                () -> buildSimpleFullTemporalInitiativeCounter(2L, 19, 1.9, trxDate)
        ));

        // 2: Charge reward limited initiative and refunded reward equal to limit
        partialRefundUseCases.add(new RefundUseCase(
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.ONE);

                    saveUserInitiativeCounter(trx, buildSimpleFullTemporalInitiativeCounter(1L, 10, 9.1, trxDate.plusDays(1)));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true,2L, 20, 10, false),
                refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.ZERO, true, 2L, 19, 10, false),
                () -> {
                    InitiativeCounters expectedCunter = buildSimpleFullTemporalInitiativeCounter(2L, 19, 10, trxDate.plusDays(1));
                    expectedCunter.getDailyCounters().put("2022-01-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(9)).totalReward(TestUtils.bigDecimalValue(0.9)).build());
                    expectedCunter.getDailyCounters().get("2022-01-02").setTrxNumber(1L);
                    expectedCunter.getDailyCounters().get("2022-01-02").setTotalAmount(TestUtils.bigDecimalValue(10));
                    expectedCunter.getDailyCounters().get("2022-01-02").setTotalReward(TestUtils.bigDecimalValue(9.1));
                    return expectedCunter;
                }
        ));

        // 3: Charge reward limited initiative and refunded reward under the limit
        partialRefundUseCases.add(new RefundUseCase(
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.valueOf(2));

                    saveUserInitiativeCounter(trx, buildSimpleFullTemporalInitiativeCounter(1L, 10, 9.1, trxDate.plusDays(1)));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true,2L, 20, 10, false),
                refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.valueOf(-0.1), true, 2L, 18, 9.9, false),
                () -> {
                    InitiativeCounters expectedCunter = buildSimpleFullTemporalInitiativeCounter(2L, 18, 9.9, trxDate.plusDays(1));
                    expectedCunter.getDailyCounters().put("2022-01-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(8)).totalReward(TestUtils.bigDecimalValue(0.8)).build());
                    expectedCunter.getDailyCounters().get("2022-01-02").setTrxNumber(1L);
                    expectedCunter.getDailyCounters().get("2022-01-02").setTotalAmount(TestUtils.bigDecimalValue(10));
                    expectedCunter.getDailyCounters().get("2022-01-02").setTotalReward(TestUtils.bigDecimalValue(9.1));
                    return expectedCunter;
                }
        ));

        // 4: Charge exhausting initiative budget TODO

        // 5: partial refunds until total TODO

        // 6: partial with reward not more present
    }

    private InitiativeCounters buildSimpleFullTemporalInitiativeCounter(long trxNumber, double totalAmount, double totalReward, OffsetDateTime trxDate) {
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
