package it.gov.pagopa.reward.dto.mapper.trx;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.trx.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.test.fakers.RewardTransactionDTOFaker;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

class RewardCountersMapperTest {

    private final RewardCountersMapper mapper = new RewardCountersMapper();

    @Test
    void test() {
        Counters expectedInvolvedDaily = new Counters(1L, 1_00L, 10_00L);
        Counters expectedInvolvedWeekly = new Counters(2L, 10_00L, 1_00L);
        Counters expectedInvolvedMonthly = new Counters(3L, 0L, 1_00L);
        Counters expectedInvolvedYearly = new Counters(4L, 10_00L, 0L);

        test(expectedInvolvedDaily, expectedInvolvedWeekly, expectedInvolvedMonthly, expectedInvolvedYearly);
    }

    @Test
    void testNoTemporalData(){
        test(null, null, null, null);
    }

    private void test(Counters expectedInvolvedDaily, Counters expectedInvolvedWeekly, Counters expectedInvolvedMonthly, Counters expectedInvolvedYearly) {
        // Given
        String dayKey = "2000-07-20";
        String weekKey = "2000-07-3";
        String monthKey = "2000-07";
        String yearKey = "2000";


        UserInitiativeCounters userInitiativeCounters = new UserInitiativeCounters();
        userInitiativeCounters.setVersion(11L);
        userInitiativeCounters.setExhaustedBudget(true);
        userInitiativeCounters.setTrxNumber(3L);
        userInitiativeCounters.setTotalRewardCents(1_00L);
        userInitiativeCounters.setTotalAmountCents(10_00L);
        userInitiativeCounters.setDailyCounters(buildTemporalCounter(expectedInvolvedDaily, dayKey));
        userInitiativeCounters.setWeeklyCounters(buildTemporalCounter(expectedInvolvedWeekly, weekKey));
        userInitiativeCounters.setMonthlyCounters(buildTemporalCounter(expectedInvolvedMonthly, monthKey));
        userInitiativeCounters.setYearlyCounters(buildTemporalCounter(expectedInvolvedYearly, yearKey));

        RewardTransactionDTO reward = RewardTransactionDTOFaker.mockInstance(0);
        reward.setTrxChargeDate(OffsetDateTime.of(LocalDate.of(2000, 7, 20), LocalTime.NOON, ZoneOffset.UTC));

        InitiativeConfig initiative = new InitiativeConfig();
        initiative.setBeneficiaryBudgetCents(10_00L);

        // When
        RewardCounters result = mapper.apply(userInitiativeCounters, reward, initiative);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertSame(userInitiativeCounters.getVersion(), result.getVersion());

        Assertions.assertSame(initiative.getBeneficiaryBudgetCents(), result.getInitiativeBudgetCents());
        Assertions.assertSame(userInitiativeCounters.isExhaustedBudget(), result.isExhaustedBudget());

        Assertions.assertSame(userInitiativeCounters.getTrxNumber(), result.getTrxNumber());
        Assertions.assertSame(userInitiativeCounters.getTotalRewardCents(), result.getTotalRewardCents());
        Assertions.assertSame(userInitiativeCounters.getTotalAmountCents(), result.getTotalAmountCents());

        Assertions.assertEquals(expectedInvolvedDaily!=null ? Map.of(dayKey, expectedInvolvedDaily) : null, result.getDailyCounters());
        Assertions.assertEquals(expectedInvolvedWeekly!=null ? Map.of(weekKey, expectedInvolvedWeekly) : null, result.getWeeklyCounters());
        Assertions.assertEquals(expectedInvolvedMonthly!=null ? Map.of(monthKey, expectedInvolvedMonthly) : null, result.getMonthlyCounters());
        Assertions.assertEquals(expectedInvolvedYearly!=null ? Map.of(yearKey, expectedInvolvedYearly) : null, result.getYearlyCounters());

        String[] nullableFields=new String[]{"", "", "", ""};
        if(expectedInvolvedDaily==null){
            nullableFields[0] = "dailyCounters";
        }
        if(expectedInvolvedWeekly==null){
            nullableFields[1] = "weeklyCounters";
        }
        if(expectedInvolvedMonthly==null){
            nullableFields[2] = "monthlyCounters";
        }
        if(expectedInvolvedYearly==null){
            nullableFields[3] = "yearlyCounters";
        }
        TestUtils.checkNotNullFields(result, nullableFields);
    }

    private static Map<String, Counters> buildTemporalCounter(Counters expectedInvolvedCounter, String key) {
        if(expectedInvolvedCounter!=null) {
            return Map.of("NOTINVOLEDKEY", new Counters(), key, expectedInvolvedCounter);
        } else {
            return Map.of("NOTINVOLEDKEY", new Counters());
        }
    }
}
