package it.gov.pagopa.reward.connector.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.common.kafka.utils.KafkaConstants;
import it.gov.pagopa.common.utils.CommonConstants;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.TransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

class TransactionProcessorRefundTest extends BaseTransactionProcessorTest {

    final LocalDateTime trxDateLocalDateTime = LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(0, 0));
    final OffsetDateTime trxDate = OffsetDateTime.of(trxDateLocalDateTime, CommonConstants.ZONEID.getRules().getOffset(trxDateLocalDateTime));
    final LocalDate initiativeStartDate = LocalDate.of(1970,1,1);

    private final String initiativeId = "INITIATIVEID";
    private final String initiative2totalRefundId = "INITIATIVE2TOTALREFUND";
    private final String initiativeTrxMinId = "INITIATIVEMINCOUNTID";

    private final List<Runnable> assertionsAfterChecks = new ArrayList<>();

    /**
     * Configuring just one initiative which will reward the 10% of the effective amount having temporal reward limits
     */
    private final InitiativeReward2BuildDTO initiative = InitiativeReward2BuildDTOFaker.mockInstanceBuilder(1, Collections.emptySet(), RewardValueDTO.class)
            .initiativeId(initiativeId)
            .initiativeName("NAME_"+initiativeId)
            .organizationId("ORGANIZATIONID_"+initiativeId)
            .general(InitiativeGeneralDTO.builder()
                    .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                    .beneficiaryBudget(BigDecimal.valueOf(100))
                    .startDate(initiativeStartDate)
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
                    .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                    .beneficiaryBudget(BigDecimal.valueOf(100))
                    .startDate(initiativeStartDate)
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
                    .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                    .beneficiaryBudget(BigDecimal.valueOf(100))
                    .startDate(initiativeStartDate)
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

        trxs.forEach(t -> onboardHpan(t.getHpan(), t.getUserId(), trxDateLocalDateTime, trxDateLocalDateTime.plusDays(10), initiativeId));

        long timePublishOnboardingStart = System.currentTimeMillis();
        trxs.forEach(i -> kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardProcessorRequest, null, i.getUserId(), i));
        kafkaTestUtilitiesService.publishIntoEmbeddedKafka(topicRewardProcessorRequest, List.of(new RecordHeader(KafkaConstants.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");
        long timePublishingOnboardingRequest = System.currentTimeMillis() - timePublishOnboardingStart;

        long timeConsumerResponse = System.currentTimeMillis();
        List<ConsumerRecord<String, String>> payloadConsumed = kafkaTestUtilitiesService.consumeMessages(topicRewardProcessorOutcome, trxs.size(), maxWaitingMs);
        long timeEnd = System.currentTimeMillis();

        long timeConsumerResponseEnd = timeEnd - timeConsumerResponse;
        Assertions.assertEquals(trxs.size(), payloadConsumed.size());

        for (ConsumerRecord<String, String> p : payloadConsumed) {
            RewardTransactionDTO payload = objectMapper.readValue(p.value(), RewardTransactionDTO.class);
            checkResponse(payload, totalRefundedTrxs);
            Assertions.assertEquals(payload.getUserId(), p.key());
        }

        assertionsAfterChecks.forEach(Runnable::run);

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
                    Math.floorMod(bias, (isTotalRefundUseCase ? totalRefundUseCases.size(): partialRefundUseCases.size())),
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

        useCase.chargeRewardVerifier().accept(rewardedTrx);
    }

    private void checkRefundOp(RewardTransactionDTO rewardedTrx, boolean isTotalRefundUseCase, int bias) {
        Assertions.assertEquals(OperationType.REFUND, rewardedTrx.getOperationTypeTranscoded());

        List<RefundUseCase> refundUseCases = isTotalRefundUseCase ? totalRefundUseCases : partialRefundUseCases;
        refundUseCases.get(bias % refundUseCases.size()).refundVerifier().accept(rewardedTrx);
    }

    private void checkUserInitiativeCounter(RewardTransactionDTO rewardedTrx, boolean isTotalRefundUseCase, int bias) {
        final List<UserInitiativeCounters> userInitiativesCounters = userInitiativeCountersRepositorySpy.findByEntityId(rewardedTrx.getUserId()).collectList().block();
        Assertions.assertNotNull(userInitiativesCounters);
        Assertions.assertFalse(userInitiativesCounters.isEmpty());

        List<RefundUseCase> refundUseCases = isTotalRefundUseCase ? totalRefundUseCases : partialRefundUseCases;
        final int useCase = bias % refundUseCases.size();
        RefundUseCase refundUseCase = refundUseCases.get(useCase);

        UserInitiativeCounters userInitiativeCounters = userInitiativesCounters.stream().filter(c -> c.getInitiativeId().equals(refundUseCase.initiativeId2Test)).findFirst().orElse(null);
        Assertions.assertNotNull(userInitiativeCounters);

        refundUseCase.expectedInitiativeFinalCounterSupplier().get().forEach(initiativeCounter ->
                {
                    UserInitiativeCounters actual = userInitiativesCounters.stream().filter(c -> c.getInitiativeId().equals(initiativeCounter.getInitiativeId())).findFirst().orElse(null);
                    UserInitiativeCounters expected = initiativeCounter.toBuilder().entityId(rewardedTrx.getUserId()).build();
                    Assertions.assertNotNull(actual);
                    Assertions.assertEquals(expected.getVersion(), actual.getVersion());
                    Assertions.assertEquals(expected.getTrxNumber(), actual.getTrxNumber());
                    Assertions.assertEquals(expected.getTotalReward(), actual.getTotalReward());
                    Assertions.assertEquals(expected.getId(), actual.getId());
                    Assertions.assertEquals(expected.getEntityId(), actual.getEntityId());
                    Assertions.assertEquals(expected.getInitiativeId(), actual.getInitiativeId());
                    Assertions.assertEquals(expected.getUpdateDate().truncatedTo(ChronoUnit.DAYS), actual.getUpdateDate().truncatedTo(ChronoUnit.DAYS));
                    Assertions.assertEquals(expected.getUpdateDate().truncatedTo(ChronoUnit.DAYS), actual.getUpdateDate().truncatedTo(ChronoUnit.DAYS));
                    Assertions.assertEquals(expected.isExhaustedBudget(), actual.isExhaustedBudget());
                    Assertions.assertEquals(expected.getDailyCounters(), actual.getDailyCounters());
                    Assertions.assertEquals(expected.getWeeklyCounters(), actual.getWeeklyCounters());
                    Assertions.assertEquals(expected.getMonthlyCounters(), actual.getMonthlyCounters());
                    Assertions.assertEquals(expected.getYearlyCounters(), actual.getYearlyCounters());
                });
    }

    private record RefundUseCase(
            String initiativeId2Test,
            Function<Integer, List<TransactionDTO>> useCaseBuilder,
            Consumer<RewardTransactionDTO> chargeRewardVerifier,
            Consumer<RewardTransactionDTO> refundVerifier,
            Supplier<List<UserInitiativeCounters>> expectedInitiativeFinalCounterSupplier) {
    }

    /**
     * it will build a transaction having an amount of 10
     */
    private static TransactionDTO buildChargeTrx(OffsetDateTime trxDate, Integer i) {
        final TransactionDTO trx = TransactionDTOFaker.mockInstance(i);
        trx.setTrxDate(trxDate);
        trx.setAmount(TestUtils.bigDecimalValue(10_00));
        return trx;
    }

    private static TransactionDTO buildRefundTrx(Integer i, TransactionDTO trx) {
        return trx.toBuilder()
                .operationType("01")
                .trxDate(trx.getTrxDate().plusDays(i + 1))
                .build();
    }

    private static UserInitiativeCounters setCounterVersion(UserInitiativeCounters counter, long version){
        counter.setVersion(version);
        return counter;
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
                () -> List.of(buildSimpleFullTemporalInitiativeCounter(0L, 0, 0, trxDate)))
        );

        // 1: Counter already initiated: as use case 0, but the user has already a counter, thus checking that after the refund it has been restored
        final UserInitiativeCounters useCaseCounterAlreadyInitiated_initialStateOfCounters =
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
                () -> List.of(setCounterVersion(useCaseCounterAlreadyInitiated_initialStateOfCounters, 4L))
        ));

        // 2: Charge reward limited initiative: as use case 1, but the user's counter now triggers the weekly reward limit leading to a reward of 0.9
        final UserInitiativeCounters useCaseChargeRewardLimited_initialStateOfCounters =
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
                () -> {
                    useCaseChargeRewardLimited_initialStateOfCounters.setVersion(4L);
                    useCaseChargeRewardLimited_initialStateOfCounters.getDailyCounters().put("2022-01-01", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    return List.of(useCaseChargeRewardLimited_initialStateOfCounters);
                }
        ));

        // 3: Charge exhausting initiative budget: as use case 1, but the user's counter now triggers the exhaustion of the beneficiary initiative budget; after refund the status of the initiative should be restored (not more exhausted)
        final UserInitiativeCounters useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters =
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
                () -> {
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.setVersion(4L);
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.getDailyCounters().put("2022-01-01", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.getWeeklyCounters().put("2022-01-0", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.getMonthlyCounters().put("2022-01", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters.getYearlyCounters().put("2022", Counters.builder().trxNumber(0L).totalAmount(TestUtils.bigDecimalValue(0)).totalReward(TestUtils.bigDecimalValue(0)).build());
                    return List.of(useCaseChargeExhaustingInitiativeBudget_initialStateOfCounters);
                }
        ));

        // 4: Charge reward limited initiative: as use case 1, but the charge itself is already capped to 0
        final UserInitiativeCounters useCaseChargeRewardLimitedCapped_initialStateOfCounters =
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
                () -> List.of(useCaseChargeRewardLimitedCapped_initialStateOfCounters)
        ));

        // 5: trxMin case never reached
        final UserInitiativeCounters useCaseTrxCountMinNeverReached_initialStateOfCounters = buildSimpleFullTemporalInitiativeCounter(4L, "USERID", initiativeTrxMinId, 3L, 10, 8, trxDate.plusDays(11));
        totalRefundUseCases.add(new RefundUseCase(
                initiativeTrxMinId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate.plusDays(11), i);
                    trx.setAmount(BigDecimal.valueOf(100_00));

                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());

                    onboardHpan(trx.getHpan(), trx.getUserId(), trx.getTrxDate().toLocalDateTime(), null, initiativeTrxMinId);
                    saveUserInitiativeCounter(trx, useCaseTrxCountMinNeverReached_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 4L, 10, 8, false, false, false),
                refundReward -> assertRejectedState(refundReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 3L, 10, 8, false, true, true),
                () -> List.of(setCounterVersion(useCaseTrxCountMinNeverReached_initialStateOfCounters, 6L))
        ));

        // 6: trxMin reached then refunded
        final UserInitiativeCounters useCaseTrxCountMinReached_initialStateOfCounters = buildSimpleFullTemporalInitiativeCounter(5L, "USERID", initiativeTrxMinId, 4L, 10, 8, trxDate.plusDays(11));
        totalRefundUseCases.add(new RefundUseCase(
                initiativeTrxMinId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate.plusDays(11), i);
                    trx.setAmount(BigDecimal.valueOf(100_00));

                    final TransactionDTO totalRefund = buildRefundTrx(i, trx);
                    totalRefund.setAmount(trx.getAmount());

                    onboardHpan(trx.getHpan(), trx.getUserId(), trx.getTrxDate().toLocalDateTime(), null, initiativeTrxMinId);
                    saveUserInitiativeCounter(trx, useCaseTrxCountMinReached_initialStateOfCounters);

                    return List.of(trx, totalRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeTrxMinId, TestUtils.bigDecimalValue(10), false, 5L, 110, 18,  false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeTrxMinId, BigDecimal.valueOf(-10), false, 4L, 10, 8, false, true, true),
                () -> List.of(setCounterVersion(useCaseTrxCountMinReached_initialStateOfCounters, 7L))
        ));

        // 7: initiatives counter's based behavior limit (https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/506266636/Mappatura+Regole#Limitazione-gestione-storni-su-iniziative-basate-su-contatori)
        // trxMin reached after N transactions, then refunding one the previous
        // due to the documented limitation, the IDTRXACQUIRER_5 transactions will still be rewarded even after the refund of the first trx (thus IDTRXACQUIRER_5 will be the 4th trx)
        // it will also test the budget exhaustion
        totalRefundUseCases.add(new RefundUseCase(
                initiativeTrxMinId,
                i -> {
                    final TransactionDTO trx1 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx1.setIdTrxAcquirer("IDTRXACQUIRER");
                    trx1.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx2 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx2.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_2");
                    trx2.setCorrelationId(trx1.getCorrelationId()+"_2");
                    trx2.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx3 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx3.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_3");
                    trx3.setCorrelationId(trx1.getCorrelationId()+"_3");
                    trx3.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx4 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx4.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_4");
                    trx4.setCorrelationId(trx1.getCorrelationId()+"_4");
                    trx4.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx5 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx5.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_5");
                    trx5.setCorrelationId(trx1.getCorrelationId()+"_5");
                    trx5.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx6 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx6.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_6");
                    trx6.setCorrelationId(trx1.getCorrelationId()+"_6");
                    trx6.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx7 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx7.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_7");
                    trx7.setCorrelationId(trx1.getCorrelationId()+"_7");
                    trx7.setAmount(BigDecimal.valueOf(1000_00));

                    final TransactionDTO trx1TF = buildRefundTrx(i, trx1);
                    trx1TF.setAmount(trx1.getAmount());

                    final TransactionDTO trx7TF = buildRefundTrx(i, trx7);
                    trx7TF.setAmount(trx7.getAmount());

                    onboardHpan(trx1.getHpan(), trx1.getUserId(), trx1.getTrxDate().toLocalDateTime(), null, initiativeTrxMinId);

                    return List.of(trx1, trx2, trx3, trx4, trx5, trx6, trx7, trx1TF, trx7TF);
                },
                chargeReward -> {
                    switch (chargeReward.getIdTrxAcquirer()) {
                        case "IDTRXACQUIRER" -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 1L, 0, 0, false, false, false);
                        case "IDTRXACQUIRER_2" -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 2L, 0, 0, false, false, false);
                        case "IDTRXACQUIRER_3" -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 3L, 0, 0, false, false, false);
                        case "IDTRXACQUIRER_4" -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 4L, 0, 0, false, false, false);
                        case "IDTRXACQUIRER_5" -> assertRewardedState(chargeReward, initiativeTrxMinId, TestUtils.bigDecimalValue(10), false, 5L, 100, 10, false);
                        case "IDTRXACQUIRER_6" -> assertRewardedState(chargeReward, initiativeTrxMinId, TestUtils.bigDecimalValue(10), false, 6L, 200, 20, false);
                        case "IDTRXACQUIRER_7" -> assertRewardedState(chargeReward, initiativeTrxMinId, TestUtils.bigDecimalValue(80), true, 7L, 1200, 100, true);
                        default -> throw new IllegalStateException("Unexpected case! " + chargeReward);
                    }
                },
                refundReward -> {
                    switch (refundReward.getIdTrxAcquirer()) {
                        case "IDTRXACQUIRER" -> assertRejectedState(refundReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 6L, 1200, 100, true, true, true);
                        case "IDTRXACQUIRER_7" -> assertRewardedState(refundReward, 1, initiativeTrxMinId, TestUtils.bigDecimalValue(-80), false, 5L, 200, 20, false, true, true);
                        default -> throw new IllegalStateException("Unexpected case! " + refundReward);
                    }
                },
                () -> List.of(buildSimpleInitiativeCounter(9L, "USERID", initiativeTrxMinId, 5L, 200, 20, false))
        ));

        // 8: refund trx came before correlated charge
        Map<String, AtomicInteger> trx2RefundCounts = new ConcurrentHashMap<>();
        totalRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO chargeTrx = buildChargeTrx(trxDate, i);
                    chargeTrx.setAmount(BigDecimal.valueOf(19_00));

                    final TransactionDTO totalRefund = buildRefundTrx(i, chargeTrx);
                    totalRefund.setAmount(chargeTrx.getAmount());

                    return List.of(totalRefund, chargeTrx, chargeTrx); // sending twice the charge operation (the second would be skipped without having be processed) in order to take in consideration the re-publish of discarded refund when counting the trx to await in output
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1.9), false, 1L, 19, 1.9,  false),
                refundReward -> {
                    AtomicInteger counter = trx2RefundCounts.computeIfAbsent(refundReward.getId(), trxId -> new AtomicInteger(0));
                    int count = counter.incrementAndGet();
                    if (refundReward.getRejectionReasons().equals(List.of(RewardConstants.TRX_REJECTION_REASON_REFUND_NOT_MATCH))) {
                        assertRejectedTrx(refundReward, List.of("REFUND_NOT_MATCH"));
                        Assertions.assertEquals(1, count);
                    } else {
                        assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-1.9), false, 0L, 0, 0, false, true, true);
                        Assertions.assertEquals(2, count);
                    }
                },
                () -> List.of(buildSimpleFullTemporalInitiativeCounter(2L, "USERID", initiativeId, 0L, 0, 0, trxDate))
        ));
        assertionsAfterChecks.add(() ->
                Assertions.assertEquals(Collections.emptyList(), trx2RefundCounts.entrySet().stream().filter(e->e.getValue().get() != 2).toList()));
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
                    partialRefund.setAmount(BigDecimal.valueOf(1_00));
                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false, 1L, 10, 1, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.1), true, 1L, 9, 0.9, false, true, false),
                () -> List.of(buildSimpleFullTemporalInitiativeCounter(1L, 9, 0.9, trxDate))
        ));

        // 1: Counter already initiated: as use case 0, but the user has already a counter, thus checking that after the partial refund its amount and reward are updated (the trx number is still +1)
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);
                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.valueOf(1_00));

                    saveUserInitiativeCounter(trx,
                            buildSimpleFullTemporalInitiativeCounter(1L, 10, 1, trxDate)
                    );

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1), false, 2L, 20, 2, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.1), true, 2L, 19, 1.9, false, true, false),
                () -> List.of(buildSimpleFullTemporalInitiativeCounter(4L, "USERID", initiativeId, 2L, 19, 1.9, trxDate))
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
                    partialRefund.setAmount(BigDecimal.valueOf(1_00));

                    saveUserInitiativeCounter(trx, buildSimpleFullTemporalInitiativeCounter(1L, 10, 9.1, trxDate.plusDays(1)));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true, 2L, 20, 10, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.ZERO, true, 2L, 19, 10, false, true, false),
                () -> {
                    UserInitiativeCounters expectedCounter = buildSimpleFullTemporalInitiativeCounter(2L, 19, 10, trxDate.plusDays(1));
                    expectedCounter.setVersion(4L);
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
                    partialRefund.setAmount(BigDecimal.valueOf(2_00));

                    saveUserInitiativeCounter(trx, buildSimpleFullTemporalInitiativeCounter(1L, 10, 9.1, trxDate.plusDays(1)));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true, 2L, 20, 10, false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-0.1), true, 2L, 18, 9.9, false, true, false),
                () -> {
                    UserInitiativeCounters expectedCounter = buildSimpleFullTemporalInitiativeCounter(2L, 18, 9.9, trxDate.plusDays(1));
                    expectedCounter.setVersion(4L);
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
                    partialRefund.setAmount(BigDecimal.valueOf(1_00));

                    saveUserInitiativeCounter(trx,
                            // placing a rewarded day of 99.9 to the previous year in order to avoid reward limits on current
                            buildSimpleFullTemporalInitiativeCounter(1L, 1000, 99.1, trxDate.minusYears(1)));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true, 2L, 1010, 100, true),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, BigDecimal.ZERO, true, 2L, 1009, 100, true, true, false),
                () -> {
                    UserInitiativeCounters expectedCounter = buildSimpleFullTemporalInitiativeCounter(1L, 9, 0.9, trxDate);
                    expectedCounter.setVersion(4L);
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
                    partialRefund.setAmount(BigDecimal.valueOf(2_00));

                    saveUserInitiativeCounter(trx,
                            // placing a rewarded day of 99.9 to the previous year in order to avoid reward limits on current
                            buildSimpleFullTemporalInitiativeCounter(1L, 1000, 99.1, trxDate.minusYears(1)));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(0.9), true, 2L, 1010, 100, true),
                refundReward -> assertRewardedState(refundReward, 1, initiativeId, TestUtils.bigDecimalValue(-0.1), true, 2L, 1008, 99.9, false, true, false),
                () -> {
                    UserInitiativeCounters expectedCounter = buildSimpleFullTemporalInitiativeCounter(1L, 8, 0.8, trxDate);
                    expectedCounter.setVersion(4L);
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
                    partialRefund.setAmount(BigDecimal.valueOf(1_00));

                    onboardHpan(trx.getHpan(), trx.getUserId(), trx.getTrxDate().toLocalDateTime(), null, initiative2totalRefund.getInitiativeId());

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
                () -> {
                    UserInitiativeCounters baseInitiativeCounter = buildSimpleFullTemporalInitiativeCounter(1L, 9, 0.9, trxDate);
                    UserInitiativeCounters initiative2totalTefundIdCounter = UserInitiativeCounters.builder("USERID", InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiative2totalRefundId)
                            .version(2L)
                            .initiativeId(initiative2totalRefundId).build();
                    return List.of(baseInitiativeCounter, initiative2totalTefundIdCounter);
                }
        ));

        // 7: partial refunds until total: as use case 0, but there will be more than 1 refund until complete amount reverted
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate, i);

                    final TransactionDTO partialRefund1 = buildRefundTrx(i, trx);
                    partialRefund1.setAmount(BigDecimal.valueOf(1_00));
                    partialRefund1.setIdTrxAcquirer("REFUND1");

                    final TransactionDTO partialRefund2 = buildRefundTrx(i, trx);
                    partialRefund2.setAmount(BigDecimal.valueOf(7_00));
                    partialRefund2.setIdTrxAcquirer("REFUND2");

                    final TransactionDTO partialRefund3 = buildRefundTrx(i, trx);
                    partialRefund3.setAmount(BigDecimal.valueOf(2_00));
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
                () -> List.of(buildSimpleFullTemporalInitiativeCounter(4L, "USERID", initiativeId, 0L, 0, 0, trxDate))
        ));

        // 8: Partial refund of charge transaction capped to 0
        final UserInitiativeCounters useCaseChargeRewardLimitedCapped_initialStateOfCounters =
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
                () -> List.of(useCaseChargeRewardLimitedCapped_initialStateOfCounters)
        ));

        // 9: trxMin case never reached
        partialRefundUseCases.add(new RefundUseCase(
                initiativeTrxMinId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate.plusDays(11), i);
                    trx.setAmount(BigDecimal.valueOf(101_00));

                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.valueOf(1_00));

                    onboardHpan(trx.getHpan(), trx.getUserId(), trx.getTrxDate().toLocalDateTime(), null, initiativeTrxMinId);
                    saveUserInitiativeCounter(trx, buildSimpleInitiativeCounter(trx.getUserId(), initiativeTrxMinId, 3L, 10, 8));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 4L, 10, 8, false, false, false),
                refundReward -> assertRejectedState(refundReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 4L, 10, 8, false, true, false),
                () -> List.of(buildSimpleInitiativeCounter(4L, "USERID", initiativeTrxMinId, 4L, 10, 8, false))
        ));

        // 10: trxMin reached then refunded
        partialRefundUseCases.add(new RefundUseCase(
                initiativeTrxMinId,
                i -> {
                    final TransactionDTO trx = buildChargeTrx(trxDate.plusDays(11), i);
                    trx.setAmount(BigDecimal.valueOf(101_00));

                    final TransactionDTO partialRefund = buildRefundTrx(i, trx);
                    partialRefund.setAmount(BigDecimal.valueOf(1_00));

                    onboardHpan(trx.getHpan(), trx.getUserId(), trx.getTrxDate().toLocalDateTime(), null, initiativeTrxMinId);
                    saveUserInitiativeCounter(trx, buildSimpleInitiativeCounter(trx.getUserId(), initiativeTrxMinId, 4L, 10, 8));

                    return List.of(trx, partialRefund);
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeTrxMinId, TestUtils.bigDecimalValue(10.1), false, 5L, 111, 18.1,  false),
                refundReward -> assertRewardedState(refundReward, 1, initiativeTrxMinId, BigDecimal.valueOf(-0.1), true, 5L, 110, 18, false, true, false),
                () -> List.of(buildSimpleInitiativeCounter(4L, "USERID", initiativeTrxMinId, 5L, 110, 18, false))
        ));

        // 11: initiatives counter's based behavior limit (https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/506266636/Mappatura+Regole#Limitazione-gestione-storni-su-iniziative-basate-su-contatori)
        // trxMin reached after N transactions, then refunding one the previous
        // due to the documented limitation, the IDTRXACQUIRER_5 transactions will still be rewarded even after the refund of the first trx (thus IDTRXACQUIRER_5 will be the 4th trx)
        // it will also test the budget exhaustion
        partialRefundUseCases.add(new RefundUseCase(
                initiativeTrxMinId,
                i -> {
                    final TransactionDTO trx1 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx1.setIdTrxAcquirer("IDTRXACQUIRER");
                    trx1.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx2 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx2.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_2");
                    trx2.setCorrelationId(trx1.getCorrelationId()+"_2");
                    trx2.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx3 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx3.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_3");
                    trx3.setCorrelationId(trx1.getCorrelationId()+"_3");
                    trx3.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx4 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx4.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_4");
                    trx4.setCorrelationId(trx1.getCorrelationId()+"_4");
                    trx4.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx5 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx5.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_5");
                    trx5.setCorrelationId(trx1.getCorrelationId()+"_5");
                    trx5.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx6 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx6.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_6");
                    trx6.setCorrelationId(trx1.getCorrelationId()+"_6");
                    trx6.setAmount(BigDecimal.valueOf(100_00));
                    final TransactionDTO trx7 = buildChargeTrx(trxDate.plusDays(11), i);
                    trx7.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"_7");
                    trx7.setCorrelationId(trx1.getCorrelationId()+"_7");
                    trx7.setAmount(BigDecimal.valueOf(1000_00));

                    final TransactionDTO trx1PF1 = buildRefundTrx(i, trx1);
                    trx1PF1.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"-PF1");
                    trx1PF1.setAmount(BigDecimal.valueOf(99_99));

                    final TransactionDTO trx1PF2 = buildRefundTrx(i, trx1);
                    trx1PF2.setIdTrxAcquirer(trx1.getIdTrxAcquirer()+"-PF2");
                    trx1PF2.setAmount(BigDecimal.valueOf(1));

                    final TransactionDTO trx7PF = buildRefundTrx(i, trx7);
                    trx7PF.setAmount(trx7.getAmount());

                    onboardHpan(trx1.getHpan(), trx1.getUserId(), trx1.getTrxDate().toLocalDateTime(), null, initiativeTrxMinId);

                    return List.of(trx1, trx2, trx3, trx4, trx5, trx6, trx7, trx1PF1, trx7PF, trx1PF2);
                },
                chargeReward -> {
                    switch (chargeReward.getIdTrxAcquirer()) {
                        case "IDTRXACQUIRER" -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 1L, 0, 0, false, false, false);
                        case "IDTRXACQUIRER_2" -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 2L, 0, 0, false, false, false);
                        case "IDTRXACQUIRER_3" -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 3L, 0, 0, false, false, false);
                        case "IDTRXACQUIRER_4" -> assertRejectedState(chargeReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 4L, 0, 0, false, false, false);
                        case "IDTRXACQUIRER_5" -> assertRewardedState(chargeReward, initiativeTrxMinId, TestUtils.bigDecimalValue(10), false, 5L, 100, 10, false);
                        case "IDTRXACQUIRER_6" -> assertRewardedState(chargeReward, initiativeTrxMinId, TestUtils.bigDecimalValue(10), false, 6L, 200, 20, false);
                        case "IDTRXACQUIRER_7" -> assertRewardedState(chargeReward, initiativeTrxMinId, TestUtils.bigDecimalValue(80), true, 7L, 1200, 100, true);
                        default -> throw new IllegalStateException("Unexpected case! " + chargeReward);
                    }
                },
                refundReward -> {
                    switch (refundReward.getIdTrxAcquirer()) {
                        case "IDTRXACQUIRER-PF1" -> assertRejectedState(refundReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 7L, 1200, 100, true, true, false);
                        case "IDTRXACQUIRER_7" -> assertRewardedState(refundReward, 1, initiativeTrxMinId, TestUtils.bigDecimalValue(-80), false, 6L, 200, 20, false, true, true);
                        case "IDTRXACQUIRER-PF2" -> assertRejectedState(refundReward, initiativeTrxMinId, List.of("TRX_RULE_TRXCOUNT_FAIL"), 5L, 200, 20, false, true, true);
                        default -> throw new IllegalStateException("Unexpected case! " + refundReward);
                    }
                },
                () -> List.of(buildSimpleInitiativeCounter(10L, "USERID", initiativeTrxMinId, 5L, 200, 20, false))
        ));

        // 12: refund trx came before correlated charge
        Map<String, AtomicInteger> trx2RefundCounts = new ConcurrentHashMap<>();
        partialRefundUseCases.add(new RefundUseCase(
                initiativeId,
                i -> {
                    final TransactionDTO chargeTrx = buildChargeTrx(trxDate, i);
                    chargeTrx.setIdTrxAcquirer("IDTRXACQUIRER");
                    chargeTrx.setAmount(BigDecimal.valueOf(19_00));

                    final TransactionDTO partialRefund1 = buildRefundTrx(i, chargeTrx);
                    partialRefund1.setIdTrxAcquirer(chargeTrx.getIdTrxAcquirer()+"_PF1");
                    partialRefund1.setAmount(BigDecimal.valueOf(10_00));

                    final TransactionDTO partialRefund2 = buildRefundTrx(i, chargeTrx);
                    partialRefund2.setIdTrxAcquirer(chargeTrx.getIdTrxAcquirer()+"_PF2");
                    partialRefund2.setAmount(BigDecimal.valueOf(1_00));

                    return List.of(partialRefund1, chargeTrx, partialRefund2, chargeTrx); // sending twice the charge operation (the second would be skipped without having be processed) in order to take in consideration the re-publish of discarded refund when counting the trx to await in output
                },
                chargeReward -> assertRewardedState(chargeReward, initiativeId, TestUtils.bigDecimalValue(1.9), false, 1L, 19, 1.9,  false),
                refundReward -> {
                    switch (refundReward.getIdTrxAcquirer()){
                        case "IDTRXACQUIRER_PF1" -> {
                            AtomicInteger counter = trx2RefundCounts.computeIfAbsent(refundReward.getId(), trxId -> new AtomicInteger(0));
                            int count = counter.incrementAndGet();
                            if (refundReward.getRejectionReasons().equals(List.of(RewardConstants.TRX_REJECTION_REASON_REFUND_NOT_MATCH))) {
                                assertRejectedTrx(refundReward, List.of("REFUND_NOT_MATCH"));
                                Assertions.assertEquals(1, count);
                            } else {
                                assertRewardedState(refundReward, 1, initiativeId, BigDecimal.valueOf(-1), true, 1L, 8, 0.8, false, true, false);
                                Assertions.assertEquals(2, count);
                            }
                        }
                        case "IDTRXACQUIRER_PF2" -> assertRewardedState(refundReward, 1, initiativeId, TestUtils.bigDecimalValue(-0.1), true, 1L, 18, 1.8,  false, true, false);
                        default -> throw new IllegalStateException("Unexpected case! " + refundReward);
                    }
                },
                () -> List.of(buildSimpleFullTemporalInitiativeCounter(3L, "USERID", initiativeId, 1L, 8, 0.8, trxDate))
        ));
        assertionsAfterChecks.add(() ->
                Assertions.assertEquals(Collections.emptyList(), trx2RefundCounts.entrySet().stream().filter(e->e.getValue().get() != 2).toList()));
    }

    private static UserInitiativeCounters buildSimpleInitiativeCounter(String userId, String initiativeId, long trxNumber, double totalAmount, double totalReward) {
        return buildSimpleInitiativeCounter(2L, userId, initiativeId, trxNumber, totalAmount, totalReward, false);
    }
    private static UserInitiativeCounters buildSimpleInitiativeCounter(long version, String userId, String initiativeId, long trxNumber, double totalAmount, double totalReward, boolean budgetExhausted) {
        return UserInitiativeCounters.builder(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId)
                .version(version)
                .trxNumber(trxNumber)
                .totalAmount(TestUtils.bigDecimalValue(totalAmount))
                .totalReward(TestUtils.bigDecimalValue(totalReward))
                .exhaustedBudget(budgetExhausted)
                .build();
    }

    private UserInitiativeCounters buildSimpleFullTemporalInitiativeCounter(long trxNumber, double totalAmount, double totalReward, OffsetDateTime trxDate) {
        return buildSimpleFullTemporalInitiativeCounter(2L, trxNumber, totalAmount, totalReward, trxDate);
    }
    private UserInitiativeCounters buildSimpleFullTemporalInitiativeCounter(long version, long trxNumber, double totalAmount, double totalReward, OffsetDateTime trxDate) {
        return buildSimpleFullTemporalInitiativeCounter(version, "USERID", initiativeId, trxNumber, totalAmount, totalReward, trxDate);
    }

    private UserInitiativeCounters buildSimpleFullTemporalInitiativeCounter(long version, String userId, String initiativeId, long trxNumber, double totalAmount, double totalReward, OffsetDateTime trxDate) {
        BigDecimal totalAmountBigDecimal = TestUtils.bigDecimalValue(totalAmount);
        BigDecimal totalRewardBigDecimal = TestUtils.bigDecimalValue(totalReward);
        return UserInitiativeCounters.builder(userId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF,initiativeId)
                .version(version)
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
