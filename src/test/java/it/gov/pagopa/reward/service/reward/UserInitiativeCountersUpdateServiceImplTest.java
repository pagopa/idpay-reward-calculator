package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserInitiativeCountersUpdateServiceImplTest {

    @Test
    void testUpdateCounters() {

        // Given
        DroolsContainerHolderService droolsContainerHolderService = Mockito.mock(DroolsContainerHolderServiceImpl.class);
        Map<String, Reward> rewardMock = Map.of("1", new Reward(BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder().amount(BigDecimal.valueOf(100)).rewards(rewardMock).build();

        InitiativeCounters initiativeCountersMock = new InitiativeCounters();
        initiativeCountersMock.setInitiativeId("1");
        initiativeCountersMock.setTrxNumber(20L);
        initiativeCountersMock.setTotalReward(BigDecimal.valueOf(200));
        initiativeCountersMock.setTotalAmount(BigDecimal.valueOf(4000));
        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "1",
                List.of(initiativeCountersMock)
        );

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);
        Mockito.when(droolsContainerHolderService.getInitiativeConfig(Mockito.any())).thenReturn(initiativeConfig);

        UserInitiativeCountersUpdateServiceImpl userInitiativeCountersUpdateService = new UserInitiativeCountersUpdateServiceImpl(droolsContainerHolderService);

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        assertEquals("1", userInitiativeCounters.getUserId());
        assertEquals("1", userInitiativeCounters.getInitiatives().get(0).getInitiativeId());
        assertEquals(21L, userInitiativeCounters.getInitiatives().get(0).getTrxNumber());
        assertEquals(BigDecimal.valueOf(250), userInitiativeCounters.getInitiatives().get(0).getTotalReward());
        assertEquals(BigDecimal.valueOf(4100), userInitiativeCounters.getInitiatives().get(0).getTotalAmount());
    }

    @Test
    void testWithPeriodicalInitiative() {

        // Given
        DroolsContainerHolderService droolsContainerHolderService = Mockito.mock(DroolsContainerHolderServiceImpl.class);
        Map<String, Reward> rewardMock = Map.of("1", new Reward(BigDecimal.valueOf(50), BigDecimal.valueOf(50), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder().amount(BigDecimal.valueOf(100)).rewards(rewardMock).build();

        InitiativeCounters initiativeCountersMock = new InitiativeCounters();
        initiativeCountersMock.setInitiativeId("1");
        initiativeCountersMock.setTrxNumber(20L);
        initiativeCountersMock.setTotalReward(BigDecimal.valueOf(200));
        initiativeCountersMock.setTotalAmount(BigDecimal.valueOf(4000));

        String dayFormattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String monthFormattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String yearFormattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));

        initiativeCountersMock.setDailyCounters(Map.of(dayFormattedDate, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build()));
        initiativeCountersMock.setMonthlyCounters(Map.of(monthFormattedDate, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build()));
        initiativeCountersMock.setYearlyCounters(Map.of(yearFormattedDate, Counters.builder().trxNumber(10L).totalReward(BigDecimal.valueOf(70)).totalAmount(BigDecimal.valueOf(100)).build()));

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "1",
                List.of(initiativeCountersMock)
        );

        Map<String, Counters> dailyCounterMock = Map.of(dayFormattedDate, new Counters(11L, BigDecimal.valueOf(120), BigDecimal.valueOf(200)));
        Map<String, Counters> monthlyCounterMock = Map.of(monthFormattedDate, new Counters(11L, BigDecimal.valueOf(120), BigDecimal.valueOf(200)));
        Map<String, Counters> yearlyCounterMock = Map.of(yearFormattedDate, new Counters(11L, BigDecimal.valueOf(120), BigDecimal.valueOf(200)));

        InitiativeConfig initiativeConfig = new InitiativeConfig(
                "1",
                true,
                true,
                true
        );
        Mockito.when(droolsContainerHolderService.getInitiativeConfig(Mockito.any())).thenReturn(initiativeConfig);

        UserInitiativeCountersUpdateServiceImpl userInitiativeCountersUpdateService = new UserInitiativeCountersUpdateServiceImpl(droolsContainerHolderService);

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        assertEquals("1", userInitiativeCounters.getUserId());
        assertEquals("1", userInitiativeCounters.getInitiatives().get(0).getInitiativeId());
        assertEquals(21L, userInitiativeCounters.getInitiatives().get(0).getTrxNumber());
        assertEquals(BigDecimal.valueOf(250), userInitiativeCounters.getInitiatives().get(0).getTotalReward());
        assertEquals(BigDecimal.valueOf(4100), userInitiativeCounters.getInitiatives().get(0).getTotalAmount());
        assertEquals(dailyCounterMock, userInitiativeCounters.getInitiatives().get(0).getDailyCounters());
        assertEquals(monthlyCounterMock, userInitiativeCounters.getInitiatives().get(0).getMonthlyCounters());
        assertEquals(yearlyCounterMock, userInitiativeCounters.getInitiatives().get(0).getYearlyCounters());
    }

    @Test
    void testWithUnrewardedInitiative() {

        // Given
        DroolsContainerHolderService droolsContainerHolderService = Mockito.mock(DroolsContainerHolderServiceImpl.class);
        Map<String, Reward> rewardMock = Map.of("2", new Reward(BigDecimal.valueOf(50), BigDecimal.valueOf(50), false), "3", new Reward(BigDecimal.ZERO, BigDecimal.ZERO, false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder().amount(BigDecimal.valueOf(100)).rewards(rewardMock).build();

        InitiativeCounters initiativeCountersMock1 = new InitiativeCounters();
        initiativeCountersMock1.setInitiativeId("1");
        initiativeCountersMock1.setTrxNumber(20L);
        initiativeCountersMock1.setTotalReward(BigDecimal.valueOf(200));
        initiativeCountersMock1.setTotalAmount(BigDecimal.valueOf(4000));

        InitiativeCounters initiativeCountersMock2 = new InitiativeCounters();
        initiativeCountersMock2.setInitiativeId("2");
        initiativeCountersMock2.setTrxNumber(20L);
        initiativeCountersMock2.setTotalReward(BigDecimal.valueOf(200));
        initiativeCountersMock2.setTotalAmount(BigDecimal.valueOf(4000));

        InitiativeCounters initiativeCountersMock3 = new InitiativeCounters();
        initiativeCountersMock3.setInitiativeId("3");
        initiativeCountersMock3.setTrxNumber(20L);
        initiativeCountersMock3.setTotalReward(BigDecimal.valueOf(200));
        initiativeCountersMock3.setTotalAmount(BigDecimal.valueOf(4000));

        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "1",
                List.of(initiativeCountersMock1, initiativeCountersMock2, initiativeCountersMock3)
        );

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);
        Mockito.when(droolsContainerHolderService.getInitiativeConfig(Mockito.any())).thenReturn(initiativeConfig);

        UserInitiativeCountersUpdateServiceImpl userInitiativeCountersUpdateService = new UserInitiativeCountersUpdateServiceImpl(droolsContainerHolderService);

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        assertEquals("1", userInitiativeCounters.getUserId());
            // First initiative (not rewarded)
        assertNull(rewardTransactionDTO.getRewards().get("1"));
        assertEquals("1", userInitiativeCounters.getInitiatives().get(0).getInitiativeId());
        assertEquals(20L, userInitiativeCounters.getInitiatives().get(0).getTrxNumber());
        assertEquals(BigDecimal.valueOf(200), userInitiativeCounters.getInitiatives().get(0).getTotalReward());
        assertEquals(BigDecimal.valueOf(4000), userInitiativeCounters.getInitiatives().get(0).getTotalAmount());
            // Second initiative (rewarded)
        assertNotNull(rewardTransactionDTO.getRewards().get("2"));
        assertEquals("2", userInitiativeCounters.getInitiatives().get(1).getInitiativeId());
        assertEquals(21L, userInitiativeCounters.getInitiatives().get(1).getTrxNumber());
        assertEquals(BigDecimal.valueOf(250), userInitiativeCounters.getInitiatives().get(1).getTotalReward());
        assertEquals(BigDecimal.valueOf(4100), userInitiativeCounters.getInitiatives().get(1).getTotalAmount());
            // Third initiate (reward is 0)
        assertNotNull(rewardTransactionDTO.getRewards().get("3"));
        assertEquals("3", userInitiativeCounters.getInitiatives().get(2).getInitiativeId());
        assertEquals(20L, userInitiativeCounters.getInitiatives().get(2).getTrxNumber());
        assertEquals(BigDecimal.valueOf(200), userInitiativeCounters.getInitiatives().get(2).getTotalReward());
        assertEquals(BigDecimal.valueOf(4000), userInitiativeCounters.getInitiatives().get(2).getTotalAmount());
    }

    @Test
    void testWithRejectionReasonCountFail() {

        // Given
        DroolsContainerHolderService droolsContainerHolderService = Mockito.mock(DroolsContainerHolderServiceImpl.class);
        Map<String, Reward> rewardMock = Map.of("1", new Reward(BigDecimal.valueOf(0), BigDecimal.valueOf(0), false));
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder().amount(BigDecimal.valueOf(100)).rewards(rewardMock).rejectionReason(List.of("TRX_RULE_TRXCOUNT_FAIL")).build();

        InitiativeCounters initiativeCountersMock = new InitiativeCounters();
        initiativeCountersMock.setInitiativeId("1");
        initiativeCountersMock.setTrxNumber(20L);
        initiativeCountersMock.setTotalReward(BigDecimal.valueOf(200));
        initiativeCountersMock.setTotalAmount(BigDecimal.valueOf(4000));
        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters(
                "1",
                List.of(initiativeCountersMock)
        );

        InitiativeConfig initiativeConfig = Mockito.mock(InitiativeConfig.class);
        Mockito.when(droolsContainerHolderService.getInitiativeConfig(Mockito.any())).thenReturn(initiativeConfig);

        UserInitiativeCountersUpdateServiceImpl userInitiativeCountersUpdateService = new UserInitiativeCountersUpdateServiceImpl(droolsContainerHolderService);

        // When
        userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        assertEquals(1, rewardTransactionDTO.getRejectionReason().size());
        assertEquals("TRX_RULE_TRXCOUNT_FAIL", rewardTransactionDTO.getRejectionReason().get(0));

        assertEquals("1", userInitiativeCounters.getUserId());
        assertEquals("1", userInitiativeCounters.getInitiatives().get(0).getInitiativeId());
        assertEquals(21L, userInitiativeCounters.getInitiatives().get(0).getTrxNumber());
        assertEquals(BigDecimal.valueOf(200), userInitiativeCounters.getInitiatives().get(0).getTotalReward());
        assertEquals(BigDecimal.valueOf(4100), userInitiativeCounters.getInitiatives().get(0).getTotalAmount());
    }
}