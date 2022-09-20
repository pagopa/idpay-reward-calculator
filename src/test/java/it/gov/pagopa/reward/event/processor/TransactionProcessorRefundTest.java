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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

class TransactionProcessorRefundTest extends BaseTransactionProcessorTest {

    private final String initiativeId = "INITIATIVEID";

    @Test
    void test() throws JsonProcessingException {
        int completeRefundedTrxs = 10;
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

        List<TransactionDTO> trxs = new ArrayList<>(IntStream.range(0, completeRefundedTrxs).mapToObj(this::buildTotalRefundRequests).flatMap(List::stream).toList());

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
            checkResponse(objectMapper.readValue(p.value(), RewardTransactionDTO.class), completeRefundedTrxs);
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

        checkOffsets(trxs.size(), trxs.size());
    }

    private List<TransactionDTO> buildTotalRefundRequests(int bias) {
        return totalRefundUseCases.get(bias % totalRefundUseCases.size()).useCaseBuilder().apply(bias);
    }

    private void checkResponse(RewardTransactionDTO rewardedTrx, int completeRefundedTrxs) {
        String hpan = rewardedTrx.getHpan();
        int biasRetrieve = Integer.parseInt(hpan.substring(4));

        if (rewardedTrx.getOperationType().equals("00")) {
            checkChargeOp(rewardedTrx, biasRetrieve < completeRefundedTrxs, biasRetrieve % completeRefundedTrxs);
        } else {
            if (biasRetrieve < completeRefundedTrxs) {
                checkTotalRefundOp(rewardedTrx, biasRetrieve);
            }
        }
        checkUserInitiativeCounter(rewardedTrx, biasRetrieve < completeRefundedTrxs, biasRetrieve % completeRefundedTrxs);
    }

    private void checkChargeOp(RewardTransactionDTO rewardedTrx, boolean totalRefundUseCase, int bias) {
        Assertions.assertEquals(OperationType.CHARGE, rewardedTrx.getOperationTypeTranscoded());
        Assertions.assertEquals(Collections.emptyList(), rewardedTrx.getRejectionReasons());
        Assertions.assertEquals(Collections.emptyMap(), rewardedTrx.getInitiativeRejectionReasons());
        Assertions.assertEquals("REWARDED", rewardedTrx.getStatus());
        if (totalRefundUseCase) {
            totalRefundUseCases.get(bias % totalRefundUseCases.size()).chargeRewardVerifier().accept(rewardedTrx);
        }
    }

    private void checkTotalRefundOp(RewardTransactionDTO rewardedTrx, int bias) {
        Assertions.assertEquals(OperationType.REFUND, rewardedTrx.getOperationTypeTranscoded());
        Assertions.assertEquals("REWARDED", rewardedTrx.getStatus());
        totalRefundUseCases.get(bias % totalRefundUseCases.size()).totalRefundVerifier().accept(rewardedTrx);
    }

    private void checkUserInitiativeCounter(RewardTransactionDTO rewardedTrx, boolean totalRefundUseCase, int bias) {
        final UserInitiativeCounters userInitiativeCounters = userInitiativeCountersRepository.findById(rewardedTrx.getUserId()).block();
        Assertions.assertNotNull(userInitiativeCounters);
        InitiativeCounters initiativeCounters = userInitiativeCounters.getInitiatives().get(initiativeId);
        Assertions.assertNotNull(initiativeCounters);
        if (totalRefundUseCase) {
            final int useCase = bias % totalRefundUseCases.size();
            Assertions.assertEquals(
                    totalRefundUseCases.get(useCase).expectedInitiativeCounterSupplier().get(),
                    initiativeCounters,
                    "Failed user initiative counter compare on user %s (case %s %d)"
                            .formatted(rewardedTrx.getUserId(),
                                    totalRefundUseCase? "TOTAL_REFUND" : "PARTIAL_REFUND",
                                    useCase)
            );
        }
    }

    //region total refund useCases
    private record CompleteRefundUseCase(
            Function<Integer, List<TransactionDTO>> useCaseBuilder,
            Consumer<RewardTransactionDTO> chargeRewardVerifier,
            Consumer<RewardTransactionDTO> totalRefundVerifier,
            Supplier<InitiativeCounters> expectedInitiativeCounterSupplier) {
    }

    private final List<CompleteRefundUseCase> totalRefundUseCases = new ArrayList<>();

    {
        final LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(0, 0));
        final OffsetDateTime trxDate = OffsetDateTime.of(localDateTime, ZoneId.of("Europe/Rome").getRules().getOffset(localDateTime));

        // 0: Base use case
        totalRefundUseCases.add(new CompleteRefundUseCase(
                i -> {
                    final TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
                    trx.setAmount(TestUtils.bigDecimalValue(10));
                    trx.setTrxDate(trxDate);
                    final TransactionDTO totalRefund = TransactionDTOFaker.mockInstance(i);
                    totalRefund.setOperationType("01");
                    totalRefund.setTrxDate(trx.getTrxDate().plusDays(i + 1));
                    totalRefund.setAmount(trx.getAmount());
                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false),
                refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.valueOf(-1), false),
                () -> InitiativeCounters.builder()
                        .initiativeId(initiativeId)
                        .trxNumber(0L)
                        .totalAmount(TestUtils.bigDecimalValue(0))
                        .totalReward(TestUtils.bigDecimalValue(0))
                        .dailyCounters(new HashMap<>(Map.of("2022-01-01", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build())))
                        .weeklyCounters(new HashMap<>(Map.of("2022-01-0", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build())))
                        .monthlyCounters(new HashMap<>(Map.of("2022-01", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build())))
                        .yearlyCounters(new HashMap<>(Map.of("2022", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build())))
                        .build()
        ));

        // 1: Counter already initiated
        final InitiativeCounters useCaseCounterAlreadyInitiated_initialStateOfCounters = InitiativeCounters.builder()
                .initiativeId(initiativeId)
                .trxNumber(1L)
                .totalAmount(TestUtils.bigDecimalValue(10))
                .totalReward(TestUtils.bigDecimalValue(1))
                .dailyCounters(new HashMap<>(Map.of("2022-01-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(10)).totalReward(TestUtils.bigDecimalValue(1)).build())))
                .weeklyCounters(new HashMap<>(Map.of("2022-01-0", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(10)).totalReward(TestUtils.bigDecimalValue(1)).build())))
                .monthlyCounters(new HashMap<>(Map.of("2022-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(10)).totalReward(TestUtils.bigDecimalValue(1)).build())))
                .yearlyCounters(new HashMap<>(Map.of("2022", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(10)).totalReward(TestUtils.bigDecimalValue(1)).build())))
                .build();
        totalRefundUseCases.add(new CompleteRefundUseCase(
                i -> {
                    final TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
                    trx.setTrxDate(trxDate);
                    trx.setAmount(TestUtils.bigDecimalValue(10));
                    final TransactionDTO totalRefund = TransactionDTOFaker.mockInstance(i);
                    totalRefund.setOperationType("01");
                    totalRefund.setTrxDate(trx.getTrxDate().plusDays(i + 1));
                    totalRefund.setAmount(trx.getAmount());

                    saveUserInitiativeCounter(trx, useCaseCounterAlreadyInitiated_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false),
                refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.valueOf(-1), false),
                () -> useCaseCounterAlreadyInitiated_initialStateOfCounters
        ));

        // 2: Charge exhausting initiative budget
        final InitiativeCounters useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters = InitiativeCounters.builder()
                .initiativeId(initiativeId)
                .trxNumber(1L)
                .totalAmount(TestUtils.bigDecimalValue(10))
                .totalReward(TestUtils.bigDecimalValue(9.1))
                .dailyCounters(new HashMap<>(Map.of("2022-01-2", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(10)).totalReward(TestUtils.bigDecimalValue(9.1)).build())))
                .weeklyCounters(new HashMap<>(Map.of("2022-01-0", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(10)).totalReward(TestUtils.bigDecimalValue(9.1)).build())))
                .monthlyCounters(new HashMap<>(Map.of("2022-01", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(10)).totalReward(TestUtils.bigDecimalValue(9.1)).build())))
                .yearlyCounters(new HashMap<>(Map.of("2022", Counters.builder().trxNumber(1L).totalAmount(TestUtils.bigDecimalValue(10)).totalReward(TestUtils.bigDecimalValue(9.1)).build())))
                .build();
        totalRefundUseCases.add(new CompleteRefundUseCase(
                i -> {
                    final TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
                    trx.setTrxDate(trxDate);
                    trx.setAmount(TestUtils.bigDecimalValue(10));
                    final TransactionDTO totalRefund = TransactionDTOFaker.mockInstance(i);
                    totalRefund.setOperationType("01");
                    totalRefund.setTrxDate(trx.getTrxDate().plusDays(i + 1));
                    totalRefund.setAmount(trx.getAmount());

                    saveUserInitiativeCounter(trx, useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true),
                refundReward -> assertRewardedState(refundReward, initiativeId, BigDecimal.valueOf(-0.9), false),
                () -> {
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.getDailyCounters().put("2022-01-01", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    return useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters;
                }
        ));
    }
    //endregion
}
