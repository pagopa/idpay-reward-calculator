package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void configureMocks(){
        initiativeConfig = InitiativeConfig.builder()
                .initiativeId("INITIATIVEID1")
                .budget(BigDecimal.valueOf(10000.00))
                .dailyThreshold(true)
                .weeklyThreshold(true)
                .monthlyThreshold(true)
                .yearlyThreshold(true)
                .build();
        Mockito.when(rewardContextHolderService.getInitiativeConfig(Mockito.any())).thenReturn(initiativeConfig);
    }

    @Test
    void testUpdateCountersWhenNeverInitiated() {
        //Given
        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward(BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .trxDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getDailyCounters().get(TRX_DATE_DAY), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getWeeklyCounters().get(TRX_DATE_WEEK), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getMonthlyCounters().get(TRX_DATE_MONTH), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getYearlyCounters().get(TRX_DATE_YEAR), 1L, 50.0, 100.0);
    }

    private void checkCounters(Counters counter, long expectedTrxCount, double expectedTotalReward, double expectedTotalAmount) {
        assertEquals(expectedTrxCount, counter.getTrxNumber());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedTotalReward), counter.getTotalReward());
        TestUtils.assertBigDecimalEquals(BigDecimal.valueOf(expectedTotalAmount), counter.getTotalAmount());
    }

    @Test
    void testUpdateCounters() {
        // Given
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward(BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .trxDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        InitiativeCounters initiativeCounters = new InitiativeCounters("INITIATIVEID1");
        initiativeCounters.setTrxNumber(20L);
        initiativeCounters.setTotalReward(BigDecimal.valueOf(200));
        initiativeCounters.setTotalAmount(BigDecimal.valueOf(4000));

        initiativeCounters.setDailyCounters(new HashMap<>(Map.of(TRX_DATE_DAY, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build())));
        initiativeCounters.setWeeklyCounters(new HashMap<>(Map.of(TRX_DATE_WEEK, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build())));
        initiativeCounters.setMonthlyCounters(new HashMap<>(Map.of(TRX_DATE_MONTH, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build())));
        initiativeCounters.setYearlyCounters(new HashMap<>(Map.of(TRX_DATE_YEAR, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build())));

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "USERID",
                new HashMap<>(Map.of(initiativeCounters.getInitiativeId(), initiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(initiativeCounters, 21L, 250, 4100);
        checkCounters(initiativeCounters.getDailyCounters().get(TRX_DATE_DAY), 11L, 120, 200);
        checkCounters(initiativeCounters.getWeeklyCounters().get(TRX_DATE_WEEK), 11L, 120, 200);
        checkCounters(initiativeCounters.getMonthlyCounters().get(TRX_DATE_MONTH), 11L, 120, 200);
        checkCounters(initiativeCounters.getYearlyCounters().get(TRX_DATE_YEAR), 11L, 120, 200);
    }

    @Test
    void testWithUnrewardedInitiative() {
        // Given
        Map<String, Reward> rewardMock = Map.of("2", new Reward(BigDecimal.valueOf(50), BigDecimal.valueOf(50), false), "3", new Reward(BigDecimal.ZERO, BigDecimal.ZERO, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .trxDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .rewards(rewardMock)
                .build();

        InitiativeCounters initiativeCounters1 = new InitiativeCounters("1");
        initiativeCounters1.setTrxNumber(20L);
        initiativeCounters1.setTotalReward(BigDecimal.valueOf(200));
        initiativeCounters1.setTotalAmount(BigDecimal.valueOf(4000));

        InitiativeCounters initiativeCounters2 = new InitiativeCounters("2");
        initiativeCounters2.setTrxNumber(20L);
        initiativeCounters2.setTotalReward(BigDecimal.valueOf(200));
        initiativeCounters2.setTotalAmount(BigDecimal.valueOf(4000));

        InitiativeCounters initiativeCounters3 = new InitiativeCounters("3");
        initiativeCounters3.setTrxNumber(20L);
        initiativeCounters3.setTotalReward(BigDecimal.valueOf(200));
        initiativeCounters3.setTotalAmount(BigDecimal.valueOf(4000));

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
        // Third initiate (reward is 0)
        checkCounters(userInitiativeCounters.getInitiatives().get("3"), 20L, 200, 4000);
    }

    @Test
    void testWithRejectionReasonCountFail() {

        // Given
        Map<String, Reward> rewardMock = Map.of(
                "2", new Reward(BigDecimal.valueOf(100), BigDecimal.valueOf(0), false),
                "3", new Reward(BigDecimal.valueOf(100), BigDecimal.valueOf(50), false)
        );
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .trxDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .rewards(rewardMock)
                .initiativeRejectionReasons(Map.of("1",List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason()))).build();

        InitiativeCounters initiativeCounters1 = new InitiativeCounters("1");
        initiativeCounters1.setTrxNumber(20L);
        initiativeCounters1.setTotalReward(BigDecimal.valueOf(200));
        initiativeCounters1.setTotalAmount(BigDecimal.valueOf(4000));

        InitiativeCounters initiativeCounters2 = new InitiativeCounters("2");
        initiativeCounters2.setTrxNumber(20L);
        initiativeCounters2.setTotalReward(BigDecimal.valueOf(200));
        initiativeCounters2.setTotalAmount(BigDecimal.valueOf(4000));
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
    }

    @Test
    void testRewardJustDailyInitiative(){
        //Given
        initiativeConfig.setWeeklyThreshold(false);
        initiativeConfig.setMonthlyThreshold(false);
        initiativeConfig.setYearlyThreshold(false);

        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward(BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .trxDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getDailyCounters().get(TRX_DATE_DAY), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getMonthlyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getYearlyCounters());
    }

    @Test
    void testRewardJustWeeklyInitiative(){
        //Given
        initiativeConfig.setDailyThreshold(false);
        initiativeConfig.setMonthlyThreshold(false);
        initiativeConfig.setYearlyThreshold(false);

        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward(BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .trxDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getWeeklyCounters().get(TRX_DATE_WEEK), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getMonthlyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getYearlyCounters());
    }

    @Test
    void testRewardJustMonthlyInitiative(){
        //Given
        initiativeConfig.setDailyThreshold(false);
        initiativeConfig.setWeeklyThreshold(false);
        initiativeConfig.setYearlyThreshold(false);

        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward(BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .trxDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getMonthlyCounters().get(TRX_DATE_MONTH), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getYearlyCounters());
    }

    @Test
    void testRewardJustYearlyInitiative(){
        //Given
        initiativeConfig.setDailyThreshold(false);
        initiativeConfig.setWeeklyThreshold(false);
        initiativeConfig.setMonthlyThreshold(false);

        UserInitiativeCounters userInitiativeCounters = UserInitiativeCounters.builder()
                .userId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward(BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .trxDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1"), 1L, 50.0, 100.0);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getMonthlyCounters());
        checkCounters(userInitiativeCounters.getInitiatives().get("INITIATIVEID1").getYearlyCounters().get(TRX_DATE_YEAR), 1L, 50.0, 100.0);
    }

    @Test
    void testRewardWithPartialAccrued() {

        // Given
        Reward reward = new Reward(BigDecimal.valueOf(2000));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .trxDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .rewards(Map.of("INITIATIVEID1", reward)).build();

        InitiativeCounters initiativeCounters = new InitiativeCounters();
        initiativeCounters.setInitiativeId("INITIATIVEID1");
        initiativeCounters.setTrxNumber(20L);
        initiativeCounters.setTotalReward(BigDecimal.valueOf(9000));
        initiativeCounters.setTotalAmount(BigDecimal.valueOf(4000));

        initiativeCounters.setDailyCounters(new HashMap<>(Map.of(TRX_DATE_DAY, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build())));
        initiativeCounters.setWeeklyCounters(new HashMap<>(Map.of(TRX_DATE_WEEK, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build())));
        initiativeCounters.setMonthlyCounters(new HashMap<>(Map.of(TRX_DATE_MONTH, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build())));
        initiativeCounters.setYearlyCounters(new HashMap<>(Map.of(TRX_DATE_YEAR, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build())));

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
        checkCounters(initiativeCounters.getDailyCounters().get(TRX_DATE_DAY), 11L, 1070, 200);
        checkCounters(initiativeCounters.getWeeklyCounters().get(TRX_DATE_WEEK), 11L, 1070, 200);
        checkCounters(initiativeCounters.getMonthlyCounters().get(TRX_DATE_MONTH), 11L, 1070, 200);
        checkCounters(initiativeCounters.getYearlyCounters().get(TRX_DATE_YEAR), 11L, 1070, 200);
    }
}