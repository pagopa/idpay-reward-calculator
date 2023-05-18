package it.gov.pagopa.reward.dto.mapper.trx.recover;

import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

class RecoveredTrx2UserInitiativeCountersMapperTest {

    private final RecoveredTrx2UserInitiativeCountersMapper mapper = new RecoveredTrx2UserInitiativeCountersMapper();

    @Test
    void test() {
        Counters expectedInvolvedDaily = new Counters(1L, BigDecimal.ONE, BigDecimal.TEN);
        Counters expectedInvolvedWeekly = new Counters(2L, BigDecimal.TEN, BigDecimal.ONE);
        Counters expectedInvolvedMonthly = new Counters(3L, BigDecimal.ZERO, BigDecimal.ONE);
        Counters expectedInvolvedYearly = new Counters(4L, BigDecimal.TEN, BigDecimal.ZERO);

        test(expectedInvolvedDaily, expectedInvolvedWeekly, expectedInvolvedMonthly, expectedInvolvedYearly, true);
    }

    @Test
    void testNoTemporalData(){
        test(null, null, null, null, true);
    }

    @Test
    void testNoTemporalDataNoPrevious(){
        test(null, null, null, null, false);
    }

    @Test
    void testNoPrevious(){
        Counters expectedInvolvedDaily = new Counters(1L, BigDecimal.ONE, BigDecimal.TEN);
        Counters expectedInvolvedWeekly = new Counters(2L, BigDecimal.TEN, BigDecimal.ONE);
        Counters expectedInvolvedMonthly = new Counters(3L, BigDecimal.ZERO, BigDecimal.ONE);
        Counters expectedInvolvedYearly = new Counters(4L, BigDecimal.TEN, BigDecimal.ZERO);

        test(expectedInvolvedDaily, expectedInvolvedWeekly, expectedInvolvedMonthly, expectedInvolvedYearly, false);
    }

    private void test(Counters expectedInvolvedDaily, Counters expectedInvolvedWeekly, Counters expectedInvolvedMonthly, Counters expectedInvolvedYearly, boolean withPrevious) {
        // Given
        String dayKey = "2000-07-20";
        String weekKey = "2000-07-3";
        String monthKey = "2000-07";
        String yearKey = "2000";


        UserInitiativeCounters previous = null;
        if(withPrevious) {
            previous = new UserInitiativeCounters();
            previous.setVersion(11L);
            previous.setExhaustedBudget(false);
            previous.setTrxNumber(3L);
            previous.setTotalReward(BigDecimal.ONE);
            previous.setTotalAmount(BigDecimal.TEN);
            previous.setDailyCounters(buildTemporalCounter(dayKey));
            previous.setWeeklyCounters(buildTemporalCounter(weekKey));
            previous.setMonthlyCounters(buildTemporalCounter(monthKey));
            previous.setYearlyCounters(buildTemporalCounter(yearKey));
        }

        Reward reward = new Reward("INITIATIVEID", "ORGANIZATIONID", BigDecimal.TEN);
        reward.setCounters(new RewardCounters());
        reward.getCounters().setVersion((previous!=null?previous.getVersion(): 0L)+1);
        reward.getCounters().setExhaustedBudget(true);
        reward.getCounters().setTrxNumber((previous!=null?previous.getTrxNumber(): 0L) + 1);
        reward.getCounters().setTotalReward((previous!=null?previous.getTotalReward(): BigDecimal.ZERO).add(BigDecimal.ONE));
        reward.getCounters().setTotalAmount((previous!=null?previous.getTotalAmount(): BigDecimal.ZERO).add(BigDecimal.ONE));
        reward.getCounters().setDailyCounters(expectedInvolvedDaily!=null ? Map.of(dayKey, expectedInvolvedDaily) : null);
        reward.getCounters().setWeeklyCounters(expectedInvolvedWeekly!=null ? Map.of(weekKey, expectedInvolvedWeekly) : null);
        reward.getCounters().setMonthlyCounters(expectedInvolvedMonthly!=null ? Map.of(monthKey, expectedInvolvedMonthly) : null);
        reward.getCounters().setYearlyCounters(expectedInvolvedYearly!=null ? Map.of(yearKey, expectedInvolvedYearly) : null);

        TransactionProcessed trxStored = new TransactionProcessed();
        trxStored.setUserId("USERID");
        trxStored.setElaborationDateTime(LocalDateTime.now());

        // When
        UserInitiativeCounters result = mapper.apply(reward, trxStored, previous);

        // Then
        Assertions.assertNotNull(result);

        Assertions.assertSame(reward.getCounters().getVersion(), result.getVersion());

        Assertions.assertEquals(UserInitiativeCounters.buildId(trxStored.getUserId(), reward.getInitiativeId()), result.getId());
        Assertions.assertEquals(trxStored.getUserId(), result.getUserId());
        Assertions.assertEquals(reward.getInitiativeId(), result.getInitiativeId());

        Assertions.assertSame(trxStored.getElaborationDateTime(), result.getUpdateDate());
        Assertions.assertSame(reward.getCounters().isExhaustedBudget(), result.isExhaustedBudget());

        Assertions.assertSame(reward.getCounters().getTrxNumber(), result.getTrxNumber());
        Assertions.assertSame(reward.getCounters().getTotalReward(), result.getTotalReward());
        Assertions.assertSame(reward.getCounters().getTotalAmount(), result.getTotalAmount());

        Assertions.assertEquals(buildExpectedTemporalCounter(reward.getCounters().getDailyCounters(), previous != null? previous.getDailyCounters(): new HashMap<>()), result.getDailyCounters());
        Assertions.assertEquals(buildExpectedTemporalCounter(reward.getCounters().getWeeklyCounters(), previous != null? previous.getWeeklyCounters(): new HashMap<>()), result.getWeeklyCounters());
        Assertions.assertEquals(buildExpectedTemporalCounter(reward.getCounters().getMonthlyCounters(), previous != null? previous.getMonthlyCounters(): new HashMap<>()), result.getMonthlyCounters());
        Assertions.assertEquals(buildExpectedTemporalCounter(reward.getCounters().getYearlyCounters(), previous != null? previous.getYearlyCounters(): new HashMap<>()), result.getYearlyCounters());

        TestUtils.checkNotNullFields(result);
    }

    private static Map<String, Counters> buildTemporalCounter(String key) {
        return Map.of("NOTINVOLEDKEY", new Counters(), key, new Counters(0L, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private Map<String, Counters> buildExpectedTemporalCounter(Map<String, Counters> involvedCounter, Map<String, Counters> previous) {
        if(involvedCounter != null){
            HashMap<String, Counters> updated = new HashMap<>(previous);
            updated.putAll(involvedCounter);
            return updated;
        } else {
            return previous;
        }
    }
}
