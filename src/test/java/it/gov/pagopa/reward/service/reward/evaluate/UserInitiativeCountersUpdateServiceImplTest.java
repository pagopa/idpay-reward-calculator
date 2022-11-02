package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.utils.TestUtils;
import it.gov.pagopa.reward.utils.RewardConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserInitiativeCountersUpdateServiceImplTest {

    public static final OffsetDateTime TRX_DATE = OffsetDateTime.of(LocalDate.of(2022, 1, 8), LocalTime.NOON, ZoneOffset.UTC);
    public static final String TRX_DATE_DAY = "2022-01-08";
    public static final String TRX_DATE_WEEK = "2022-01-1";
    public static final String TRX_DATE_MONTH = "2022-01";
    public static final String TRX_DATE_YEAR = "2022";


    @Mock
    private RewardContextHolderService rewardContextHolderService;
    @InjectMocks
    private UserInitiativeCountersUpdateServiceImpl userInitiativeCountersUpdateService;

    private InitiativeConfig initiativeConfig;

    @BeforeEach
    public void configureMocks() {
        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVEID1")
                .beneficiaryBudget(BigDecimal.valueOf(10000.00))
                .dailyThreshold(true)
                .weeklyThreshold(true)
                .monthlyThreshold(true)
                .yearlyThreshold(true)
                .build();
        Mockito.when(rewardContextHolderService.getInitiativeConfig(Mockito.any())).thenReturn(initiativeConfig);
    }

    @Test
    void testGetFormatters() {
        Assertions.assertSame(UserInitiativeCountersUpdateServiceImpl.dayDateFormatter, UserInitiativeCountersUpdateServiceImpl.getDayDateFormatter());
        Assertions.assertSame(UserInitiativeCountersUpdateServiceImpl.weekDateFormatter, UserInitiativeCountersUpdateServiceImpl.getWeekDateFormatter());
        Assertions.assertSame(UserInitiativeCountersUpdateServiceImpl.monthDateFormatter, UserInitiativeCountersUpdateServiceImpl.getMonthDateFormatter());
        Assertions.assertSame(UserInitiativeCountersUpdateServiceImpl.yearDateFormatter, UserInitiativeCountersUpdateServiceImpl.getYearDateFormatter());

        rewardContextHolderService.getInitiativeConfig(null); // useless, provided only to avoid to configure mock explicitly in each test only because this will not use it
    }

    @Test
    void testUpdateCountersWhenNeverInitiated() {
        //Given
        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1", "ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getDailyCounters().get(TRX_DATE_DAY), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getWeeklyCounters().get(TRX_DATE_WEEK), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getMonthlyCounters().get(TRX_DATE_MONTH), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getYearlyCounters().get(TRX_DATE_YEAR), 1L, 50.0, 100.0);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 1L, false, 50.0, 10000.0, 100.0);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    private void checkCounters(Counters counter, long expectedTrxCount, double expectedTotalReward, double expectedTotalAmount) {
        assertEquals(expectedTrxCount, counter.getTrxNumber());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedTotalReward), counter.getTotalReward());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedTotalAmount), counter.getTotalAmount());
    }

    private void checkRewardCounters(RewardCounters rewardCounters, long expectedTrxCount, boolean expectedExhaustedBudget, double expectedTotalReward, double expectedInitiativeBudget, double expectedTotalAmount) {
        assertEquals(expectedTrxCount, rewardCounters.getTrxNumber());
        assertEquals(expectedExhaustedBudget, rewardCounters.isExhaustedBudget());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedTotalAmount), rewardCounters.getTotalAmount());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedInitiativeBudget), rewardCounters.getInitiativeBudget());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedTotalReward), rewardCounters.getTotalReward());
    }

    @Test
    void testUpdateCounters() {
        // Given
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        InitiativeCounters initiativeCounters = createInitiativeCounter("INITIATIVEID1", 20L, 4000, 200);

        setTemporalCounters(initiativeCounters, 11L, 100, 70);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "USERID",
                new HashMap<>(Map.of(initiativeCounters.getInitiativeId(), initiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(initiativeCounters, 21L, 250, 4100);
        checkTemporaleCounters(initiativeCounters, 12L, 120, 200);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 21L, false, 250, 10000, 4100);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testUpdateCountersExhaustingInitiative() {
        // Given
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(9800)));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        InitiativeCounters initiativeCounters = createInitiativeCounter("INITIATIVEID1", 20L, 4000, 200);

        setTemporalCounters(initiativeCounters, 10L, 100, 70);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "USERID",
                new HashMap<>(Map.of(initiativeCounters.getInitiativeId(), initiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(initiativeCounters, 21L, 10000, 4100);
        checkTemporaleCounters(initiativeCounters, 11L, 9870, 200);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 21L, true, 10000, 10000, 4100);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testWithUnrewardedInitiative() {
        // Given
        Map<String, Reward> rewardMock = Map.of(
                "2", new Reward("2","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false),
                "3", new Reward("3","ORGANIZATION", BigDecimal.ZERO, BigDecimal.ZERO, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock)
                .build();

        InitiativeCounters initiativeCounters1 = createInitiativeCounter("1", 20L, 4000, 200);
        InitiativeCounters initiativeCounters2 = createInitiativeCounter("2", 20L, 4000, 200);
        InitiativeCounters initiativeCounters3 = createInitiativeCounter("3", 20L, 4000, 200);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "USERID",
                new HashMap<>(Map.of(
                        initiativeCounters1.getInitiativeId(), initiativeCounters1,
                        initiativeCounters2.getInitiativeId(), initiativeCounters2,
                        initiativeCounters3.getInitiativeId(), initiativeCounters3)
                ));

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        // First initiative (not rewarded)
        checkCounters(userInitiativeCounters.getInitiatives().get("1"), 20L, 200, 4000);
        // Second initiative (rewarded)
        checkCounters(userInitiativeCounters.getInitiatives().get("2"), 21L, 250, 4100);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("2").getCounters(), 21L, false, 250, 10000, 4100);
        // Third initiate (reward is 0)
        checkCounters(userInitiativeCounters.getInitiatives().get("3"), 20L, 200, 4000);
        Assertions.assertNull(rewardTransactionDTO.getRewards().get("3").getCounters());
        Assertions.assertFalse(rewardMock.get("2").isCompleteRefund());
        Assertions.assertFalse(rewardMock.get("3").isCompleteRefund());
    }

    @Test
    void testWithRejectionReasonCountFail() {

        // Given
        Map<String, Reward> rewardMock = Map.of(
                "2", new Reward("2","ORGANIZATION", BigDecimal.valueOf(100), BigDecimal.valueOf(0), false),
                "3", new Reward("3","ORGANIZATION", BigDecimal.valueOf(100), BigDecimal.valueOf(50), false)
        );
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock)
                .initiativeRejectionReasons(Map.of("1", List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason()))).build();

        InitiativeCounters initiativeCounters1 = createInitiativeCounter("1", 20L, 4000, 200);
        InitiativeCounters initiativeCounters2 = createInitiativeCounter("2", 20L, 4000, 200);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "USERID",
                new HashMap<>(Map.of("1", initiativeCounters1,
                        "2", initiativeCounters2))
        );


        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("1"), 20L, 200, 4000);
        checkCounters(userInitiativeCounters.getInitiatives().get("2"), 20L, 200, 4000);
        checkCounters(userInitiativeCounters.getInitiatives().get("3"), 1L, 50, 100);
        Assertions.assertFalse(rewardMock.get("2").isCompleteRefund());
        Assertions.assertFalse(rewardMock.get("3").isCompleteRefund());
    }

    private static InitiativeCounters createInitiativeCounter(String initiativeId, long trxNumber, double totalAmount, double totalReward) {
        InitiativeCounters initiativeCounters1 = new InitiativeCounters(initiativeId);
        initiativeCounters1.setTrxNumber(trxNumber);
        initiativeCounters1.setTotalAmount(BigDecimal.valueOf(totalAmount));
        initiativeCounters1.setTotalReward(BigDecimal.valueOf(totalReward));
        return initiativeCounters1;
    }

    @Test
    void testRewardJustDailyInitiative() {
        //Given
        initiativeConfig.setWeeklyThreshold(false);
        initiativeConfig.setMonthlyThreshold(false);
        initiativeConfig.setYearlyThreshold(false);

        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getDailyCounters().get(TRX_DATE_DAY), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getMonthlyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getYearlyCounters());
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testRewardJustWeeklyInitiative() {
        //Given
        initiativeConfig.setDailyThreshold(false);
        initiativeConfig.setMonthlyThreshold(false);
        initiativeConfig.setYearlyThreshold(false);

        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getWeeklyCounters().get(TRX_DATE_WEEK), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getMonthlyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getYearlyCounters());
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testRewardJustMonthlyInitiative() {
        //Given
        initiativeConfig.setDailyThreshold(false);
        initiativeConfig.setWeeklyThreshold(false);
        initiativeConfig.setYearlyThreshold(false);

        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getMonthlyCounters().get(TRX_DATE_MONTH), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getYearlyCounters());
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testRewardJustYearlyInitiative() {
        //Given
        initiativeConfig.setDailyThreshold(false);
        initiativeConfig.setWeeklyThreshold(false);
        initiativeConfig.setMonthlyThreshold(false);

        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getMonthlyCounters());
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getYearlyCounters().get(TRX_DATE_YEAR), 1L, 50.0, 100.0);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testRewardWithPartialAccrued() {

        // Given
        Reward reward = new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(2000));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmount(BigDecimal.valueOf(100))
                .rewards(Map.of("INITIATIVEID1", reward)).build();

        InitiativeCounters initiativeCounters = createInitiativeCounter("INITIATIVEID1", 20L, 4000, 9000);

        setTemporalCounters(initiativeCounters, 10L, 100, 70);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "USERID",
                new HashMap<>(Map.of(initiativeCounters.getInitiativeId(), initiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(initiativeCounters, 21L, 10000, 4100);
        assertEquals(BigDecimal.valueOf(1000.0).setScale(2, RoundingMode.HALF_DOWN), reward.getAccruedReward());
        assertTrue(reward.isCapped());
        checkTemporaleCounters(initiativeCounters, 11L, 1070, 200);
        Assertions.assertFalse(reward.isCompleteRefund());
    }

    private static void setTemporalCounters(InitiativeCounters initiativeCounters, long trxNumber, int totalAmount, int totalReward) {
        initiativeCounters.setDailyCounters(new HashMap<>(Map.of(TRX_DATE_DAY, Counters.builder().trxNumber(trxNumber).totalReward(BigDecimal.valueOf(totalReward)).totalAmount(BigDecimal.valueOf(totalAmount)).build())));
        initiativeCounters.setWeeklyCounters(new HashMap<>(Map.of(TRX_DATE_WEEK, Counters.builder().trxNumber(trxNumber).totalReward(BigDecimal.valueOf(totalReward)).totalAmount(BigDecimal.valueOf(totalAmount)).build())));
        initiativeCounters.setMonthlyCounters(new HashMap<>(Map.of(TRX_DATE_MONTH, Counters.builder().trxNumber(trxNumber).totalReward(BigDecimal.valueOf(totalReward)).totalAmount(BigDecimal.valueOf(totalAmount)).build())));
        initiativeCounters.setYearlyCounters(new HashMap<>(Map.of(TRX_DATE_YEAR, Counters.builder().trxNumber(trxNumber).totalReward(BigDecimal.valueOf(totalReward)).totalAmount(BigDecimal.valueOf(totalAmount)).build())));
    }

    @Test
    void testRewardPartialRefundNoPrevious() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(1000));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.REFUND)
                .trxDate(TRX_DATE.plusDays(1))
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(99))
                .effectiveAmount(BigDecimal.valueOf(1))
                .rewards(Map.of("INITIATIVEID1", reward1))
                .build();

        InitiativeCounters initiativeCounters = createInitiativeCounter("INITIATIVEID1", 20L, 4000, 9000);
        setTemporalCounters(initiativeCounters, 10L, 101, 1001);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters("USERID",
                new HashMap<>(Map.of("INITIATIVEID1", initiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(initiativeCounters, 21L, 10000, 4001);
        assertEquals(TestUtils.bigDecimalValue(1000), reward1.getAccruedReward());
        assertFalse(reward1.isCapped());
        checkTemporaleCounters(initiativeCounters, 11L, 2001, 102);
        Assertions.assertFalse(reward1.isCompleteRefund());
    }

    @Test
    void testRewardPartialRefund() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(-1000));
        Reward reward2 = new Reward("INITIATIVEID2","ORGANIZATION", BigDecimal.valueOf(2000));
        Reward reward3 = new Reward("INITIATIVEID3","ORGANIZATION", BigDecimal.valueOf(-2000));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .operationTypeTranscoded(OperationType.REFUND)
                .trxDate(TRX_DATE.plusDays(1))
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(99))
                .effectiveAmount(BigDecimal.valueOf(1))
                .rewards(Map.of("INITIATIVEID1", reward1, "INITIATIVEID2", reward2, "INITIATIVEID3", reward3))
                .refundInfo(RefundInfo.builder()
                        .previousRewards(Map.of(
                                "INITIATIVEID1", new RefundInfo.PreviousReward("INITIATIVEID1", "ORGANIZATION", BigDecimal.valueOf(2000)),
                                "INITIATIVEID3", new RefundInfo.PreviousReward("INITIATIVEID3", "ORGANIZATION", BigDecimal.valueOf(2000))))
                        .build())
                .build();

        InitiativeCounters initiativeCounters = createInitiativeCounter("INITIATIVEID1", 20L, 4000, 9000);

        setTemporalCounters(initiativeCounters, 10L, 101, 1001);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "USERID",
                new HashMap<>(Map.of(
                        "INITIATIVEID1", initiativeCounters,
                        "INITIATIVEID3", createInitiativeCounter("INITIATIVEID3", 1L, 100, 2000)
                ))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(initiativeCounters, 20L, 8000, 3901);
        assertEquals(BigDecimal.valueOf(-1000), reward1.getAccruedReward());
        assertFalse(reward1.isCapped());
        checkTemporaleCounters(initiativeCounters, 10L, 1, 2);
        Assertions.assertFalse(reward1.isCompleteRefund());

        //reward2
        InitiativeCounters initiativeCounters2 = userInitiativeCounters.getInitiatives().get("INITIATIVEID2");
        checkCounters(initiativeCounters2, 1L, 2000, 1);
        assertEquals(BigDecimal.valueOf(2000), reward2.getAccruedReward());
        assertFalse(reward2.isCapped());
        checkTemporaleCounters(initiativeCounters2, 1L, 2000, 1);
        Assertions.assertFalse(reward2.isCompleteRefund());

        //reward3
        InitiativeCounters initiativeCounters3 = userInitiativeCounters.getInitiatives().get("INITIATIVEID3");
        checkCounters(initiativeCounters3, 0L, 0, 0);
        Assertions.assertTrue(reward3.isCompleteRefund());
    }

    private void checkTemporaleCounters(InitiativeCounters initiativeCounters, long expectedTrxCount, int expectedTotalReward, int expectedTotalAmount) {
        checkCounters(initiativeCounters.getDailyCounters().get(TRX_DATE_DAY), expectedTrxCount, expectedTotalReward, expectedTotalAmount);
        checkCounters(initiativeCounters.getWeeklyCounters().get(TRX_DATE_WEEK), expectedTrxCount, expectedTotalReward, expectedTotalAmount);
        checkCounters(initiativeCounters.getMonthlyCounters().get(TRX_DATE_MONTH), expectedTrxCount, expectedTotalReward, expectedTotalAmount);
        checkCounters(initiativeCounters.getYearlyCounters().get(TRX_DATE_YEAR), expectedTrxCount, expectedTotalReward, expectedTotalAmount);
    }

    @Test
    void testRewardPartialRefundCappedToCharge() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.ZERO); // Capped rewards previous rewarded, should update their amount
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
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

        InitiativeCounters initiativeCounters = createInitiativeCounter("INITIATIVEID1", 20L, 4000, 9000);

        setTemporalCounters(initiativeCounters, 10L, 101, 1001);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "USERID",
                new HashMap<>(Map.of(initiativeCounters.getInitiativeId(), initiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(initiativeCounters, 20L, 9000, 3999);
        assertEquals(BigDecimal.ZERO, reward1.getAccruedReward());
        assertFalse(reward1.isCapped());
        checkTemporaleCounters(initiativeCounters, 10L, 1001, 100);
        Assertions.assertFalse(reward1.isCompleteRefund());
    }

    @Test
    void testRewardTotalRefund() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", BigDecimal.valueOf(-1000));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
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

        InitiativeCounters initiativeCounters = createInitiativeCounter("INITIATIVEID1", 20L, 4000, 9000);

        setTemporalCounters(initiativeCounters, 10L, 101, 1001);

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "USERID",
                new HashMap<>(Map.of(initiativeCounters.getInitiativeId(), initiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(initiativeCounters, 19L, 8000, 3900);
        assertEquals(BigDecimal.valueOf(-1000), reward1.getAccruedReward());
        assertFalse(reward1.isCapped());
        checkTemporaleCounters(initiativeCounters, 9L, 1, 1);
        Assertions.assertTrue(reward1.isCompleteRefund());
    }

}