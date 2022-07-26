package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserInitiativeCountersUpdateServiceImplTest {

    @Test
    void testUpdate() {

        // Given
        DroolsContainerHolderService droolsContainerHolderService = Mockito.mock(DroolsContainerHolderServiceImpl.class);
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder().amount(BigDecimal.valueOf(100)).rewards(Map.of("1", BigDecimal.valueOf(50))).build();

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
        UserInitiativeCounters result = userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        assertEquals("1", result.getUserId());
        assertEquals("1", result.getInitiatives().get(0).getInitiativeId());
        assertEquals(21L, result.getInitiatives().get(0).getTrxNumber());
        assertEquals(BigDecimal.valueOf(250), result.getInitiatives().get(0).getTotalReward());
        assertEquals(BigDecimal.valueOf(4100), result.getInitiatives().get(0).getTotalAmount());
    }

    @Test
    void testUpdateWithThresholds() {

        // Given
        DroolsContainerHolderService droolsContainerHolderService = Mockito.mock(DroolsContainerHolderServiceImpl.class);
        RewardTransactionDTO rewardTransactionDTO = RewardTransactionDTO.builder().amount(BigDecimal.valueOf(100)).rewards(Map.of("1", BigDecimal.valueOf(50))).build();

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
        UserInitiativeCounters result = userInitiativeCountersUpdateService.update(userInitiativeCounters, rewardTransactionDTO);

        // Then
        assertEquals("1", result.getUserId());
        assertEquals("1", result.getInitiatives().get(0).getInitiativeId());
        assertEquals(21L, result.getInitiatives().get(0).getTrxNumber());
        assertEquals(BigDecimal.valueOf(250), result.getInitiatives().get(0).getTotalReward());
        assertEquals(BigDecimal.valueOf(4100), result.getInitiatives().get(0).getTotalAmount());
        assertEquals(dailyCounterMock, result.getInitiatives().get(0).getDailyCounters());
        assertEquals(monthlyCounterMock, result.getInitiatives().get(0).getMonthlyCounters());
        assertEquals(yearlyCounterMock, result.getInitiatives().get(0).getYearlyCounters());
    }

}