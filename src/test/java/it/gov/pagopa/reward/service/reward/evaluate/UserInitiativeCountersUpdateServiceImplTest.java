package it.gov.pagopa.reward.service.reward.evaluate;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.mapper.trx.RewardCountersMapper;
import it.gov.pagopa.reward.dto.trx.RefundInfo;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.enums.OperationType;
import it.gov.pagopa.reward.model.BaseTransactionProcessed;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper;
import it.gov.pagopa.reward.service.reward.RewardContextHolderService;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
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
import java.time.*;
import java.util.*;
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
                .beneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.PF)
                .beneficiaryBudgetCents(10000_00L)
                .dailyThreshold(true)
                .weeklyThreshold(true)
                .monthlyThreshold(true)
                .yearlyThreshold(true)
                .build();
        Mockito.when(rewardContextHolderServiceMock.getInitiativeConfig(Mockito.any())).thenReturn(Mono.just(initiativeConfig));

        userInitiativeCountersUpdateService = new UserInitiativeCountersUpdateServiceImpl(rewardContextHolderServiceMock, new RewardCountersMapper(),"PT1H");
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
        initiativeConfig.setBeneficiaryType(InitiativeGeneralDTO.BeneficiaryTypeEnum.NF);
        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .entityId("FAMILYID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1", "ORGANIZATION", 50_00L, 50_00L, false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmountCents(100_00L)
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 50_00L, 100_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters().get(TRX_DATE_DAY), 1L, 50_00L, 100_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters().get(TRX_DATE_WEEK), 1L, 50_00L, 100_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getMonthlyCounters().get(TRX_DATE_MONTH), 1L, 50_00L, 100_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getYearlyCounters().get(TRX_DATE_YEAR), 1L, 50_00L, 100_00L);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 1L, false, 50_00L, 10000_00L, 100_00L);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    private void checkCounters(Counters counter, long expectedTrxCount, long expectedTotalRewardCents, long expectedTotalAmountCents) {
        assertEquals(expectedTrxCount, counter.getTrxNumber());
        assertEquals(expectedTotalRewardCents, counter.getTotalRewardCents());
        assertEquals(expectedTotalAmountCents, counter.getTotalAmountCents());
    }

    private void checkRewardCounters(RewardCounters rewardCounters, long expectedTrxCount, boolean expectedExhaustedBudget, long expectedTotalRewardCents, long expectedInitiativeBudgetCents, long expectedTotalAmountCents) {
        checkRewardCounters(rewardCounters, 1L, expectedTrxCount, expectedExhaustedBudget, expectedTotalRewardCents, expectedInitiativeBudgetCents, expectedTotalAmountCents);
    }
    private void checkRewardCounters(RewardCounters rewardCounters, long expectedVersion, long expectedTrxCount, boolean expectedExhaustedBudget, long expectedTotalRewardCents, long expectedInitiativeBudgetCents, long expectedTotalAmountCents) {
        assertEquals(expectedVersion, rewardCounters.getVersion());
        assertEquals(expectedTrxCount, rewardCounters.getTrxNumber());
        assertEquals(expectedExhaustedBudget, rewardCounters.isExhaustedBudget());
        assertEquals(expectedTotalAmountCents, rewardCounters.getTotalAmountCents());
        assertEquals(expectedInitiativeBudgetCents, rewardCounters.getInitiativeBudgetCents());
        assertEquals(expectedTotalRewardCents, rewardCounters.getTotalRewardCents());
    }

    @Test
    void testUpdateCounters() {
        // Given
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", 50_00L, 50_00L, false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmountCents(100_00L)
                .rewards(rewardMock).build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000_00L, 200_00L);

        setTemporalCounters(userInitiativeCounters, 11L, 100_00L, 70_00L);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 21L, 250_00L, 4100_00L);
        checkTemporalCounters(userInitiativeCounters, 12L, 120_00L, 200_00L);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 21L, false, 250_00L, 10000_00L, 4100_00L);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testUpdateCountersExhaustingInitiative() {
        // Given
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", 9800_00L));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(10000))
                .effectiveAmountCents(10000_00L)
                .rewards(rewardMock).build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000_00L, 200_00L);

        setTemporalCounters(userInitiativeCounters, 10L, 100_00L, 70_00L);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 21L, 10000_00L, 14000_00L);
        checkTemporalCounters(userInitiativeCounters, 11L, 9870_00L, 10100_00L);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 21L, true, 10000_00L, 10000_00L, 14000_00L);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testWithUnrewardedInitiative() {
        // Given
        Map<String, Reward> rewardMock = Map.of(
                "2", new Reward("2","ORGANIZATION", 50_00L, 50_00L, false, false),
                "3", new Reward("3","ORGANIZATION", 0L, 0L, false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmountCents(100_00L)
                .rewards(rewardMock)
                .build();

        UserInitiativeCounters userInitiativeCounters1 = createInitiativeCounter(rewardTransactionDTO.getUserId(), "1", 20L, 4000_00L, 200_00L);
        UserInitiativeCounters userInitiativeCounters2 = createInitiativeCounter(rewardTransactionDTO.getUserId(), "2", 20L, 4000_00L, 200_00L);
        UserInitiativeCounters userInitiativeCounters3 = createInitiativeCounter(rewardTransactionDTO.getUserId(), "3", 20L, 4000_00L, 200_00L);

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
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("1"), 20L, 200_00L, 4000_00L);
        // Second initiative (rewarded)
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("2"), 21L, 250_00L, 4100_00L);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("2").getCounters(), 21L, false, 250_00L, 10000_00L, 4100_00L);
        // Third initiate (reward is 0)
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("3"), 20L, 200_00L, 4000_00L);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("3").getCounters(), 0L, 20L, false, 200_00L, 10000_00L, 4000_00L);

        Assertions.assertFalse(rewardMock.get("2").isCompleteRefund());
        Assertions.assertFalse(rewardMock.get("3").isCompleteRefund());
    }

    @Test
    void testWithRejectionReasonCountFail() {

        // Given
        Map<String, Reward> rewardMock = Map.of(
                "1", new Reward("1","ORGANIZATION", 0L, 0L, false, false),
                "2", new Reward("2","ORGANIZATION", 100_00L, 0L, false, false),
                "3", new Reward("3","ORGANIZATION", 100_00L, 50_00L, false, false)
        );
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .amountCents(100_00L)
                .effectiveAmountCents(100_00L)
                .rewards(rewardMock)
                .initiativeRejectionReasons(Map.of("1", List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason()))).build();

        UserInitiativeCounters userInitiativeCounters1 = createInitiativeCounter(rewardTransactionDTO.getUserId(), "1", 20L, 4000_00L, 200_00L);
        UserInitiativeCounters userInitiativeCounters2 = createInitiativeCounter(rewardTransactionDTO.getUserId(), "2", 20L, 4000_00L, 200_00L);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of("1", userInitiativeCounters1,
                        "2", userInitiativeCounters2))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("1"), 21L, 200_00L, 4000_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("2"), 20L, 200_00L, 4000_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("3"), 1L, 50_00L, 100_00L);
        Assertions.assertFalse(rewardMock.get("2").isCompleteRefund());
        Assertions.assertFalse(rewardMock.get("3").isCompleteRefund());

        // When partially refunding it
        rewardTransactionDTO.setOperationTypeTranscoded(OperationType.REFUND);
        rewardTransactionDTO.setAmount(BigDecimal.valueOf(10));
        rewardTransactionDTO.setAmountCents(10_00L);
        rewardTransactionDTO.setEffectiveAmountCents(90_00L);
        rewardTransactionDTO.setRefundInfo(RefundInfo.builder()
                .previousRewards(rewardMock.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> new RefundInfo.PreviousReward(e.getValue().getInitiativeId(), e.getValue().getOrganizationId(), e.getValue().getAccruedRewardCents()))))
                .build());
        rewardTransactionDTO.setRewards(rewardMock.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e-> new Reward(e.getValue().getInitiativeId(), e.getValue().getOrganizationId(), 0L))));
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("1"), 21L, 200_00L, 4000_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("2"), 20L, 200_00L, 4000_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("3"), 1L, 50_00L, 90_00L);
        Assertions.assertFalse(rewardMock.get("2").isCompleteRefund());
        Assertions.assertFalse(rewardMock.get("3").isCompleteRefund());

        // When totally refunding it
        rewardTransactionDTO.setOperationTypeTranscoded(OperationType.REFUND);
        rewardTransactionDTO.setAmount(BigDecimal.valueOf(90));
        rewardTransactionDTO.setAmountCents(90_00L);
        rewardTransactionDTO.setEffectiveAmountCents(0L);
        rewardMock.values().forEach(r->r.setAccruedRewardCents(-r.getAccruedRewardCents()));
        rewardTransactionDTO.setRewards(rewardMock);
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("1"), 20L, 200_00L, 4000_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("2"), 20L, 200_00L, 4000_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("3"), 0L, 0, 0);
        Assertions.assertFalse(rewardMock.get("2").isCompleteRefund()); // not complete here, because it never rewarded the trx because of cap
        Assertions.assertTrue(rewardMock.get("3").isCompleteRefund());
    }

    private static UserInitiativeCounters createInitiativeCounter(String entityId, String initiativeId, long trxNumber, long totalAmountCents, long totalRewardCents) {
        UserInitiativeCounters userInitiativeCounters1 = new UserInitiativeCounters(entityId, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF, initiativeId);
        userInitiativeCounters1.setTrxNumber(trxNumber);
        userInitiativeCounters1.setTotalAmountCents(totalAmountCents);
        userInitiativeCounters1.setTotalRewardCents(totalRewardCents);
        return userInitiativeCounters1;
    }

    @Test
    void testRewardJustDailyInitiative() {
        //Given
        initiativeConfig.setWeeklyThreshold(false);
        initiativeConfig.setMonthlyThreshold(false);
        initiativeConfig.setYearlyThreshold(false);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .entityId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", 50_00L, 50_00L, false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .amountCents(100_00L)
                .effectiveAmountCents(100_00L)
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 50_00L, 100_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters().get(TRX_DATE_DAY), 1L, 50_00L, 100_00L);
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
                .entityId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", 50_00L, 50_00L, false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmountCents(100_00L)
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 50_00L, 100_00L);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters().get(TRX_DATE_WEEK), 1L, 50_00L, 100_00L);
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
                .entityId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", 50_00L, 50_00L, false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmountCents(100_00L)
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 50_00L, 100_00L);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getMonthlyCounters().get(TRX_DATE_MONTH), 1L, 50_00L, 100_00L);
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
                .entityId("USERID")
                .initiatives(new HashMap<>())
                .build();
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", 50_00L, 50_00L, false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmountCents(100_00L)
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 50_00L, 100_00L);
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters());
        Assertions.assertEquals(Collections.emptyMap(), userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getMonthlyCounters());
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getYearlyCounters().get(TRX_DATE_YEAR), 1L, 50_00L, 100_00L);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testRewardWithPartialAccrued() {

        // Given
        Reward reward = new Reward("INITIATIVEID1","ORGANIZATION", 2000_00L);
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.CHARGE)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .effectiveAmountCents(2100_00L)
                .rewards(Map.of("INITIATIVEID1", reward)).build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000_00L, 9000_00L);

        setTemporalCounters(userInitiativeCounters, 10L, 100_00L, 70_00L);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 21L, 10000_00L, 6100_00L);
        assertEquals(1000_00L, reward.getAccruedRewardCents());
        assertTrue(reward.isCapped());
        checkTemporalCounters(userInitiativeCounters, 11L, 1070_00L, 2200_00L);
        Assertions.assertFalse(reward.isCompleteRefund());
    }

    private static void setTemporalCounters(UserInitiativeCounters userInitiativeCounters, long trxNumber, long totalAmountCents, long totalRewardCents) {
        userInitiativeCounters.setDailyCounters(new HashMap<>(Map.of(TRX_DATE_DAY, Counters.builder().trxNumber(trxNumber).totalRewardCents(totalRewardCents).totalAmountCents(totalAmountCents).build())));
        userInitiativeCounters.setWeeklyCounters(new HashMap<>(Map.of(TRX_DATE_WEEK, Counters.builder().trxNumber(trxNumber).totalRewardCents(totalRewardCents).totalAmountCents(totalAmountCents).build())));
        userInitiativeCounters.setMonthlyCounters(new HashMap<>(Map.of(TRX_DATE_MONTH, Counters.builder().trxNumber(trxNumber).totalRewardCents(totalRewardCents).totalAmountCents(totalAmountCents).build())));
        userInitiativeCounters.setYearlyCounters(new HashMap<>(Map.of(TRX_DATE_YEAR, Counters.builder().trxNumber(trxNumber).totalRewardCents(totalRewardCents).totalAmountCents(totalAmountCents).build())));
    }

    @Test
    void testRewardPartialRefundNoPrevious() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", 1000_00L);
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.REFUND)
                .trxDate(TRX_DATE.plusDays(1))
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(99))
                .effectiveAmountCents(1001_00L)
                .rewards(Map.of("INITIATIVEID1", reward1))
                .build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000_00L, 9000_00L);
        setTemporalCounters(userInitiativeCounters, 10L, 101_00L, 1001_00L);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper("USERID",
                new HashMap<>(Map.of("INITIATIVEID1", userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 21L, 10000_00L, 5001_00L);
        assertEquals(1000_00L, reward1.getAccruedRewardCents());
        assertFalse(reward1.isCapped());
        checkTemporalCounters(userInitiativeCounters, 11L, 2001_00L, 1102_00L);
        Assertions.assertFalse(reward1.isCompleteRefund());
    }

    @Test
    void testRewardPartialRefund() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", -1000_00L);
        Reward reward2 = new Reward("INITIATIVEID2","ORGANIZATION", 2000_00L);
        Reward reward3 = new Reward("INITIATIVEID3","ORGANIZATION", -2000_00L);
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.REFUND)
                .trxDate(TRX_DATE.plusDays(1))
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(99))
                .amountCents(99_00L)
                .effectiveAmountCents(2001_00L)
                .rewards(Map.of("INITIATIVEID1", reward1, "INITIATIVEID2", reward2, "INITIATIVEID3", reward3))
                .refundInfo(RefundInfo.builder()
                        .previousRewards(Map.of(
                                "INITIATIVEID1", new RefundInfo.PreviousReward("INITIATIVEID1", "ORGANIZATION", 2000_00L),
                                "INITIATIVEID3", new RefundInfo.PreviousReward("INITIATIVEID3", "ORGANIZATION", 2000_00L)))
                        .build())
                .build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000_00L, 9000_00L);

        setTemporalCounters(userInitiativeCounters, 10L, 101_00L, 1001_00L);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(
                        "INITIATIVEID1", userInitiativeCounters,
                        "INITIATIVEID3", createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID3", 1L, 2100_00L, 2000_00L)
                ))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 20L, 8000_00L, 3901_00L);
        assertEquals(-1000_00L, reward1.getAccruedRewardCents());
        assertFalse(reward1.isCapped());
        checkTemporalCounters(userInitiativeCounters, 10L, 1_00L, 2_00L);
        Assertions.assertFalse(reward1.isCompleteRefund());

        //reward2
        UserInitiativeCounters userInitiativeCounters2 = userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID2");
        checkCounters(userInitiativeCounters2, 1L, 2000_00L, 2001_00L);
        assertEquals(2000_00L, reward2.getAccruedRewardCents());
        assertFalse(reward2.isCapped());
        checkTemporalCounters(userInitiativeCounters2, 1L, 2000_00L, 2001_00L);
        Assertions.assertFalse(reward2.isCompleteRefund());

        //reward3
        UserInitiativeCounters userInitiativeCounters3 = userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID3");
        checkCounters(userInitiativeCounters3, 0L, 0L, 0L);
        Assertions.assertTrue(reward3.isCompleteRefund());
    }

    private void checkTemporalCounters(UserInitiativeCounters userInitiativeCounters, long expectedTrxCount, long expectedTotalRewardCents, long expectedTotalAmountCents) {
        checkCounters(userInitiativeCounters.getDailyCounters().get(TRX_DATE_DAY), expectedTrxCount, expectedTotalRewardCents, expectedTotalAmountCents);
        checkCounters(userInitiativeCounters.getWeeklyCounters().get(TRX_DATE_WEEK), expectedTrxCount, expectedTotalRewardCents, expectedTotalAmountCents);
        checkCounters(userInitiativeCounters.getMonthlyCounters().get(TRX_DATE_MONTH), expectedTrxCount, expectedTotalRewardCents, expectedTotalAmountCents);
        checkCounters(userInitiativeCounters.getYearlyCounters().get(TRX_DATE_YEAR), expectedTrxCount, expectedTotalRewardCents, expectedTotalAmountCents);
    }

    @Test
    void testRewardPartialRefundCappedToCharge() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", 0L); // Capped rewards previous rewarded, should update their amount
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.REFUND)
                .trxDate(TRX_DATE.plusDays(1))
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.ONE)
                .amountCents(1_00L)
                .effectiveAmountCents(99_00L)
                .rewards(Map.of("INITIATIVEID1", reward1))
                .refundInfo(RefundInfo.builder()
                        .previousRewards(Map.of("INITIATIVEID1", new RefundInfo.PreviousReward("INITIATIVEID1", "ORGANIZATION", 1000_00L)))
                        .build())
                .build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000_00L, 9000_00L);

        setTemporalCounters(userInitiativeCounters, 10L, 101_00L, 1001_00L);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 20L, 9000_00L, 3999_00L);
        assertEquals(0L, reward1.getAccruedRewardCents());
        assertFalse(reward1.isCapped());
        checkTemporalCounters(userInitiativeCounters, 10L, 1001_00L, 100_00L);
        Assertions.assertFalse(reward1.isCompleteRefund());
    }

    @Test
    void testRewardTotalRefund() {
        // Given
        Reward reward1 = new Reward("INITIATIVEID1","ORGANIZATION", -1000_00L);
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder()
                .userId("USERID")
                .operationTypeTranscoded(OperationType.REFUND)
                .trxDate(TRX_DATE.plusDays(1))
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(100))
                .amountCents(100_00L)
                .effectiveAmountCents(0L)
                .rewards(Map.of("INITIATIVEID1", reward1))
                .refundInfo(RefundInfo.builder()
                        .previousRewards(Map.of("INITIATIVEID1", new RefundInfo.PreviousReward("INITIATIVEID1", "ORGANIZATION", 1000_00L)))
                        .build())
                .build();

        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000_00L, 9000_00L);

        setTemporalCounters(userInitiativeCounters, 10L, 101_00L, 1001_00L);

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCounters, 19L, 8000_00L, 3900_00L);
        assertEquals(-1000_00L, reward1.getAccruedRewardCents());
        assertFalse(reward1.isCapped());
        checkTemporalCounters(userInitiativeCounters, 9L, 1_00L, 1_00L);
        Assertions.assertTrue(reward1.isCompleteRefund());
    }

    @Test
    void testCapRewardToEffectiveAmount() {
        //Given
        UserInitiativeCountersWrapper userInitiativeCountersWrapper = UserInitiativeCountersWrapper.builder()
                .entityId("USERID")
                .initiatives(new HashMap<>())
                .build();

        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1", "ORGANIZATION", 50_00L, 50_00L, false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(0).toBuilder()
                .operationTypeTranscoded(OperationType.REFUND)
                .trxChargeDate(TRX_DATE)
                .amount(BigDecimal.valueOf(1))
                .effectiveAmountCents(10_00L)
                .rewards(rewardMock).build();

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1"), 1L, 10_00L, 10_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getDailyCounters().get(TRX_DATE_DAY), 1L, 10_00L, 10_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getWeeklyCounters().get(TRX_DATE_WEEK), 1L, 10_00L, 10_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getMonthlyCounters().get(TRX_DATE_MONTH), 1L, 10_00L, 10_00L);
        checkCounters(userInitiativeCountersWrapper.getInitiatives().get("INITIATIVEID1").getYearlyCounters().get(TRX_DATE_YEAR), 1L, 10_00L, 10_00L);
        checkRewardCounters(rewardTransactionDTO.getRewards().get("INITIATIVEID1").getCounters(), 1L, false, 10_00L, 10000_00L, 10_00L);
        Assertions.assertFalse(rewardMock.get("INITIATIVEID1").isCompleteRefund());
    }

    @Test
    void testUpdateLastTrx(){
        // Given
        Map<String, Reward> rewardMock = Map.of("INITIATIVEID1", new Reward("INITIATIVEID1","ORGANIZATION", 50_00L, 50_00L, false, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTOFaker.mockInstance(0);
        rewardTransactionDTO.setUserId("USERID");
        rewardTransactionDTO.setOperationTypeTranscoded(OperationType.CHARGE);
        rewardTransactionDTO.setTrxChargeDate(TRX_DATE);
        rewardTransactionDTO.setAmount(BigDecimal.valueOf(100));
        rewardTransactionDTO.setEffectiveAmountCents(100_00L);
        rewardTransactionDTO.setRewards(rewardMock);


        UserInitiativeCounters userInitiativeCounters = createInitiativeCounter(rewardTransactionDTO.getUserId(), "INITIATIVEID1", 20L, 4000_00L, 200_00L);

        setTemporalCounters(userInitiativeCounters, 11L, 100_00L, 70_00L);

        //set initial lastTrx
        LocalDateTime localDateTimeNow = LocalDateTime.now();
        RewardTransactionDTO trxAlreadyProcessedExpired = RewardTransactionDTOFaker.mockInstance(1);
        trxAlreadyProcessedExpired.setUserId("USERID");
        trxAlreadyProcessedExpired.setElaborationDateTime(localDateTimeNow.minusHours(2));
        RewardTransactionDTO trxAlreadyProcessedNotExpired = RewardTransactionDTOFaker.mockInstance(2);
        trxAlreadyProcessedNotExpired.setUserId("USERID");
        trxAlreadyProcessedNotExpired.setElaborationDateTime(localDateTimeNow);
        userInitiativeCounters.setLastTrx(Arrays.asList(trxAlreadyProcessedExpired, trxAlreadyProcessedNotExpired));

        UserInitiativeCountersWrapper userInitiativeCountersWrapper = new UserInitiativeCountersWrapper(
                "USERID",
                new HashMap<>(Map.of(userInitiativeCounters.getInitiativeId(), userInitiativeCounters))
        );

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCountersWrapper, rewardTransactionDTO).block();

        // Then
        List<BaseTransactionProcessed> resultLastTrx = userInitiativeCounters.getLastTrx();
        Assertions.assertEquals(2, resultLastTrx.size());
        Assertions.assertEquals(Arrays.asList(trxAlreadyProcessedNotExpired, rewardTransactionDTO), resultLastTrx);
    }

}