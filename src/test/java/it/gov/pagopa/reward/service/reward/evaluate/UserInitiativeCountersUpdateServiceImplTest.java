package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.mapper.trx.RewardCountersMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserInitiativeCountersUpdateServiceImplTest {

    public static final OffsetDateTime TRX_DATE = OffsetDateTime.of(LocalDate.of(2022, 1, 8), LocalTime.NOON, ZoneOffset.UTC);
    public static final String TRX_DATE_DAY = "2022-01-08";
    public static final String TRX_DATE_WEEK = "2022-01-1";
    public static final String TRX_DATE_MONTH = "2022-01";
    public static final String TRX_DATE_YEAR = "2022";


    @Mock
    private RewardContextHolderService rewardContextHolderServiceMock;

    private UserInitiativeCountersUpdateServiceImpl userInitiativeCountersUpdateService;

    private InitiativeConfig initiativeConfig;

    @BeforeEach
    public void init() {
        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVEID1")
                .beneficiaryBudget(BigDecimal.valueOf(10000.00))
                .dailyThreshold(true)
                .weeklyThreshold(true)
                .monthlyThreshold(true)
                .yearlyThreshold(true)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(Mockito.any())).thenReturn(Mono.just(initiativeConfig));

        userInitiativeCountersUpdateService = new UserInitiativeCountersUpdateServiceImpl(rewardContextHolderServiceMock, new RewardCountersMapper());
    }

    @Test
    void testGetFormatters() {
        Assertions.assertSame(RewardConstants.dayDateFormatter, UserInitiativeCountersUpdateServiceImpl.getDayDateFormatter());
        Assertions.assertSame(RewardConstants.weekDateFormatter, UserInitiativeCountersUpdateServiceImpl.getWeekDateFormatter());
        Assertions.assertSame(RewardConstants.monthDateFormatter, UserInitiativeCountersUpdateServiceImpl.getMonthDateFormatter());
        Assertions.assertSame(RewardConstants.yearDateFormatter, UserInitiativeCountersUpdateServiceImpl.getYearDateFormatter());

        rewardContextHolderServiceMock.getInitiativeConfig(null); // useless, provided only to avoid to configure mock explicitly in each test only because this will not use it
    }

    @Test
    void testUpdateCountersWhenNeverInitiated() {
        //Given
        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1", "ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters().get(TRX_DATE_DAY), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters().get(TRX_DATE_WEEK), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getMonthlyCounters().get(TRX_DATE_MONTH), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getYearlyCounters().get(TRX_DATE_YEAR), 1L, 50.0, 100.0);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 1L, false, 50.0, 10000.0, 100.0);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    private void checkCounters(Counters counter, long expectedTrxCount, double expectedTotalReward, double expectedTotalAmount) {
        assertEquals(expectedTrxCount, counter.getTrxNumber());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedTotalReward), counter.getTotalReward());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedTotalAmount), counter.getTotalAmount());
    }

    private void checkRewardCounters(RewardCounters rewardCounters, long expectedTrxCount, boolean expectedExhaustedBudget, double expectedTotalReward, double expectedInitiativeBudget, double expectedTotalAmount) {
        checkRewardCounters(rewardCounters, 1L, expectedTrxCount, expectedExhaustedBudget, expectedTotalReward, expectedInitiativeBudget, expectedTotalAmount);
    }
    private void checkRewardCounters(RewardCounters rewardCounters, long expectedVersion, long expectedTrxCount, boolean expectedExhaustedBudget, double expectedTotalReward, double expectedInitiativeBudget, double expectedTotalAmount) {
        assertEquals(expectedVersion, rewardCounters.getVersion());
        assertEquals(expectedTrxCount, rewardCounters.getTrxNumber());
        assertEquals(expectedExhaustedBudget, rewardCounters.isExhaustedBudget());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedTotalAmount), rewardCounters.getTotalAmount());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedInitiativeBudget), rewardCounters.getInitiativeBudget());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedTotalReward), rewardCounters.getTotalReward());
    }

    @Test
    void testUpdateCounters() {
        // Given
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000, 200);

        setTemporalCounters(userInitiativeCounters, 11L, 100, 70);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 21L, 250, 4100);
        checkTemporaleCounters(userInitiativeCounters, 12L, 120, 200);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 21L, false, 250, 10000, 4100);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testUpdateCountersExhaustingInitiative() {
        // Given
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(9800)));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(10000))
                .effectiveAmount(BigDecimal.valueOf(10000))
                .rewards(rewardMock).build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000, 200);

        setTemporalCounters(userInitiativeCounters, 10L, 100, 70);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 21L, 10000, 14000);
        checkTemporaleCounters(userInitiativeCounters, 11L, 9870, 10100);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 21L, true, 10000, 10000, 14000);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testWithUnrewardedInitiative() {
        // Given
        Map<String, Reward> rewardMock = Map.of(
                "2", new Reward("2","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false, false),
                "3", new Reward("3","ORGANIZATION", BigDecimal.ZERO, BigDecimal.ZERO, false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock)
                .build();

        UserInitiativeCounters userInitiativeCounters1 = createInitiativeCounter(rewardTransactionDTO.getUserId(), "1", 20L, 4000, 200);
        UserInitiativeCounters userInitiativeCounters2 = createInitiativeCounter(rewardTransactionDTO.getUserId(), "2", 20L, 4000, 200);
        UserInitiativeCounters userInitiativeCounters3 = createInitiativeCounter(rewardTransactionDTO.getUserId(), "3", 20L, 4000, 200);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(
                        userInitiativeCounters1.getInitiativeId(), userInitiativeCounters1,
                        userInitiativeCounters2.getInitiativeId(), userInitiativeCounters2,
                        userInitiativeCounters3.getInitiativeId(), userInitiativeCounters3)
                ));

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        // First initiative (not rewarded)
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("1"), 20L, 200, 4000);
        // Second initiative (rewarded)
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("2"), 21L, 250, 4100);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("2").getCounters(), 21L, false, 250, 10000, 4100);
        // Third initiate (reward is 0)
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("3"), 20L, 200, 4000);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("3").getCounters(), 0L, 20L, false, 200, 10000, 4000);

        Assertions.assertFalse(rewardMock.get("2").isCompleteRefund());
        Assertions.assertFalse(rewardMock.get("3").isCompleteRefund());
    }

    @Test
    void testWithRejectionReasonCountFail() {

        // Given
        Map<String, Reward> rewardMock = Map.of(
                "1", new Reward("1","ORGANIZATION", BigDecimal.valueOf(0), BigDecimal.valueOf(0), false, false),
                "2", new Reward("2","ORGANIZATION", BigDecimal.valueOf(100), BigDecimal.valueOf(0), false, false),
                "3", new Reward("3","ORGANIZATION", BigDecimal.valueOf(100), BigDecimal.valueOf(50), false, false)
        );
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock)
                .initiativeRejectionReasons(Map.of("1", List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason()))).build();

        UserInitiativeCounters userInitiativeCounters1 = createInitiativeCounter(rewardTransactionDTO.getUserId(), "1", 20L, 4000, 200);
        UserInitiativeCounters userInitiativeCounters2 = createInitiativeCounter(rewardTransactionDTO.getUserId(), "2", 20L, 4000, 200);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of("1", userInitiativeCounters1,
                        "2", userInitiativeCounters2))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("1"), 21L, 200, 4000);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("2"), 20L, 200, 4000);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("3"), 1L, 50, 100);
        Assertions.assertFalse(rewardMock.get("2").isCompleteRefund());
        Assertions.assertFalse(rewardMock.get("3").isCompleteRefund());

        // When partially refunding it
        rewardTransactionDTO.setOperationTypeTranscoded(OperationType.REFUND);
        rewardTransactionDTO.setAmount(BigDecimal.valueOf(10));
        rewardTransactionDTO.setEffectiveAmount(BigDecimal.valueOf(90));
        rewardTransactionDTO.setRefundInfo(RefundInfo.builder()
                .previousRewards(rewardMock.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> new RefundInfo.PreviousReward(e.getValue().getInitiativeId(), e.getValue().getOrganizationId(), e.getValue().getAccruedReward()))))
                .build());
        rewardTransactionDTO.setRewards(rewardMock.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e-> new Reward(e.getValue().getInitiativeId(), e.getValue().getOrganizationId(), BigDecimal.ZERO))));
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("1"), 21L, 200, 4000);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("2"), 20L, 200, 4000);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("3"), 1L, 50, 90);
        Assertions.assertFalse(rewardMock.get("2").isCompleteRefund());
        Assertions.assertFalse(rewardMock.get("3").isCompleteRefund());

        // When totally refunding it
        rewardTransactionDTO.setOperationTypeTranscoded(OperationType.REFUND);
        rewardTransactionDTO.setAmount(BigDecimal.valueOf(90));
        rewardTransactionDTO.setEffectiveAmount(BigDecimal.ZERO);
        rewardMock.values().forEach(r->r.setAccruedReward(r.getAccruedReward().negate()));
        rewardTransactionDTO.setRewards(rewardMock);
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("1"), 20L, 200, 4000);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("2"), 20L, 200, 4000);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("3"), 0L, 0, 0);
        Assertions.assertFalse(rewardMock.get("2").isCompleteRefund()); // not complete here, because it never rewarded the trx because of cap
        Assertions.assertTrue(rewardMock.get("3").isCompleteRefund());
    }

    private static UserInitiativeCounters createInitiativeCounter(String userId, String initiativeId, long trxNumber, double totalAmount, double totalReward) {
        UserInitiativeCounters userInitiativeCounters1 = new UserInitiativeCounters(userId, initiativeId);
        userInitiativeCounters1.setTrxNumber(trxNumber);
        userInitiativeCounters1.setTotalAmount(BigDecimal.valueOf(totalAmount));
        userInitiativeCounters1.setTotalReward(BigDecimal.valueOf(totalReward));
        return userInitiativeCounters1;
    }

    @Test
    void testRewardJustDailyInitiative() {
        //Given
        initiativeConfig.setWeeklyThreshold(false);
        initiativeConfig.setMonthlyThreshold(false);
        initiativeConfig.setYearlyThreshold(false);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters().get(TRX_DATE_DAY), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getMonthlyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getYearlyCounters());
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testRewardJustWeeklyInitiative() {
        //Given
        initiativeConfig.setDailyThreshold(false);
        initiativeConfig.setMonthlyThreshold(false);
        initiativeConfig.setYearlyThreshold(false);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters().get(TRX_DATE_WEEK), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getMonthlyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getYearlyCounters());
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testRewardJustMonthlyInitiative() {
        //Given
        initiativeConfig.setDailyThreshold(false);
        initiativeConfig.setWeeklyThreshold(false);
        initiativeConfig.setYearlyThreshold(false);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getMonthlyCounters().get(TRX_DATE_MONTH), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getYearlyCounters());
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testRewardJustYearlyInitiative() {
        //Given
        initiativeConfig.setDailyThreshold(false);
        initiativeConfig.setWeeklyThreshold(false);
        initiativeConfig.setMonthlyThreshold(false);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getMonthlyCounters());
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getYearlyCounters().get(TRX_DATE_YEAR), 1L, 50.0, 100.0);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testRewardWithPartialAccrued() {

        // Given
        Reward reward = new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(2000));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(2100))
                .rewards(Map.of("INITIATIVEID1", reward)).build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000, 9000);

        setTemporalCounters(userInitiativeCounters, 10L, 100, 70);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 21L, 10000, 6100);
        assertEquals(BigDecimal.valueOf(1000.0).setScale(2, RoundingMode.HALF_DOWN), reward.getAccruedReward());
        assertTrue(reward.isCapped());
        checkTemporaleCounters(userInitiativeCounters, 11L, 1070, 2200);
        Assertions.assertFalse(reward.isCompleteRefund());
    }

    private static void setTemporalCounters(UserInitiativeCounters userInitiativeCounters, long trxNumber, int totalAmount, int totalReward) {
        userInitiativeCounters.setDailyCounters(new HashMap<>(Map.of(TRX_DATE_DAY, Counters.builder().trxNumber(trxNumber).totalReward(BigDecimal.valueOf(totalReward)).totalAmount(BigDecimal.valueOf(totalAmount)).build())));
        userInitiativeCounters.setWeeklyCounters(new HashMap<>(Map.of(TRX_DATE_WEEK, Counters.builder().trxNumber(trxNumber).totalReward(BigDecimal.valueOf(totalReward)).totalAmount(BigDecimal.valueOf(totalAmount)).build())));
        userInitiativeCounters.setMonthlyCounters(new HashMap<>(Map.of(TRX_DATE_MONTH, Counters.builder().trxNumber(trxNumber).totalReward(BigDecimal.valueOf(totalReward)).totalAmount(BigDecimal.valueOf(totalAmount)).build())));
        userInitiativeCounters.setYearlyCounters(new HashMap<>(Map.of(TRX_DATE_YEAR, Counters.builder().trxNumber(trxNumber).totalReward(BigDecimal.valueOf(totalReward)).totalAmount(BigDecimal.valueOf(totalAmount)).build())));
    }

    @Test
    void testRewardPartialRefundNoPrevious() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(1000));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.REFUND)
                .trxDate(TRX_DATE.plusDays(1))
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(99))
                .effectiveAmount(BigDecimal.valueOf(1001))
                .rewards(Map.of("INITIATIVEID1", reward1))
                .build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000, 9000);
        setTemporalCounters(userInitiativeCounters, 10L, 101, 1001);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper("USERID",
                new HashMap<>(Map.of("INITIATIVEID1", userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 21L, 10000, 5001);
        assertEquals(TestUtils.bigDecimalValue(1000), reward1.getAccruedReward());
        assertFalse(reward1.isCapped());
        checkTemporaleCounters(userInitiativeCounters, 11L, 2001, 1102);
        Assertions.assertFalse(reward1.isCompleteRefund());
    }

    @Test
    void testRewardPartialRefund() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(-1000));
        Reward reward2 = new Reward("INITIATIVEID2","ORGANIZATION", BigDecimal.valueOf(2000));
        Reward reward3 = new Reward("INITIATIVEID3","ORGANIZATION", BigDecimal.valueOf(-2000));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.REFUND)
                .trxDate(TRX_DATE.plusDays(1))
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(99))
                .effectiveAmount(BigDecimal.valueOf(2001))
                .rewards(Map.of("INITIATIVEID1", reward1, "INITIATIVEID2", reward2, "INITIATIVEID3", reward3))
                .refundInfo(RefundInfo.builder()
                        .previousRewards(Map.of(
                                "INITIATIVEID1", new RefundInfo.PreviousReward("INITIATIVEID1", "ORGANIZATION", BigDecimal.valueOf(2000)),
                                "INITIATIVEID3", new RefundInfo.PreviousReward("INITIATIVEID3", "ORGANIZATION", BigDecimal.valueOf(2000))))
                        .build())
                .build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000, 9000);

        setTemporalCounters(userInitiativeCounters, 10L, 101, 1001);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(
                        "INITIATIVEID1", userInitiativeCounters,
                        "INITIATIVEID3", createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID3", 1L, 2100, 2000)
                ))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 20L, 8000, 3901);
        assertEquals(BigDecimal.valueOf(-1000), reward1.getAccruedReward());
        assertFalse(reward1.isCapped());
        checkTemporaleCounters(userInitiativeCounters, 10L, 1, 2);
        Assertions.assertFalse(reward1.isCompleteRefund());

        //reward2
        UserInitiativeCounters userInitiativeCounters2 = userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID2");
        checkCounters(userInitiativeCounters2, 1L, 2000, 2001);
        assertEquals(BigDecimal.valueOf(2000), reward2.getAccruedReward());
        assertFalse(reward2.isCapped());
        checkTemporaleCounters(userInitiativeCounters2, 1L, 2000, 2001);
        Assertions.assertFalse(reward2.isCompleteRefund());

        //reward3
        UserInitiativeCounters userInitiativeCounters3 = userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID3");
        checkCounters(userInitiativeCounters3, 0L, 0, 0);
        Assertions.assertTrue(reward3.isCompleteRefund());
    }

    private void checkTemporaleCounters(UserInitiativeCounters userInitiativeCounters, long expectedTrxCount, int expectedTotalReward, int expectedTotalAmount) {
        checkCounters(userInitiativeCounters.getDailyCounters().get(TRX_DATE_DAY), expectedTrxCount, expectedTotalReward, expectedTotalAmount);
        checkCounters(userInitiativeCounters.getWeeklyCounters().get(TRX_DATE_WEEK), expectedTrxCount, expectedTotalReward, expectedTotalAmount);
        checkCounters(userInitiativeCounters.getMonthlyCounters().get(TRX_DATE_MONTH), expectedTrxCount, expectedTotalReward, expectedTotalAmount);
        checkCounters(userInitiativeCounters.getYearlyCounters().get(TRX_DATE_YEAR), expectedTrxCount, expectedTotalReward, expectedTotalAmount);
    }

    @Test
    void testRewardPartialRefundCappedToCharge() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.ZERO); // Capped rewards previous rewarded, should update their amount
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.REFUND)
                .trxDate(TRX_DATE.plusDays(1))
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.ONE)
                .effectiveAmount(BigDecimal.valueOf(99))
                .rewards(Map.of("INITIATIVEID1", reward1))
                .refundInfo(RefundInfo.builder()
                        .previousRewards(Map.of("INITIATIVEID1", new RefundInfo.PreviousReward("INITIATIVEID1", "ORGANIZATION", BigDecimal.valueOf(1000))))
                        .build())
                .build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000, 9000);

        setTemporalCounters(userInitiativeCounters, 10L, 101, 1001);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 20L, 9000, 3999);
        assertEquals(BigDecimal.ZERO, reward1.getAccruedReward());
        assertFalse(reward1.isCapped());
        checkTemporaleCounters(userInitiativeCounters, 10L, 1001, 100);
        Assertions.assertFalse(reward1.isCompleteRefund());
    }

    @Test
    void testRewardTotalRefund() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(-1000));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.REFUND)
                .trxDate(TRX_DATE.plusDays(1))
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.ZERO)
                .rewards(Map.of("INITIATIVEID1", reward1))
                .refundInfo(RefundInfo.builder()
                        .previousRewards(Map.of("INITIATIVEID1", new RefundInfo.PreviousReward("INITIATIVEID1", "ORGANIZATION", BigDecimal.valueOf(1000))))
                        .build())
                .build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000, 9000);

        setTemporalCounters(userInitiativeCounters, 10L, 101, 1001);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 19L, 8000, 3900);
        assertEquals(BigDecimal.valueOf(-1000), reward1.getAccruedReward());
        assertFalse(reward1.isCapped());
        checkTemporaleCounters(userInitiativeCounters, 9L, 1, 1);
        Assertions.assertTrue(reward1.isCompleteRefund());
    }

    @Test
    void testCapRewardToEffectiveAmount() {
        //Given
        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();

        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1", "ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(0).toBuilder()
                .operationTypeTranscoded(OperationType.REFUND)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(1))
                .effectiveAmount(BigDecimal.valueOf(10))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 10.0, 10.0);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters().get(TRX_DATE_DAY), 1L, 10.0, 10.0);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters().get(TRX_DATE_WEEK), 1L, 10.0, 10.0);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getMonthlyCounters().get(TRX_DATE_MONTH), 1L, 10.0, 10.0);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getYearlyCounters().get(TRX_DATE_YEAR), 1L, 10.0, 10.0);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 1L, false, 10.0, 10000.0, 10.0);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

}