package it.gov.pagopa.reward.dto.mapper.trx.recover;

import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.RewardCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import it.gov.pagopa.common.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

class RecoveredTrx2UserInitiativeCountersMapperTest {

    private final RecoveredTrx2UserInitiativeCountersMapper mapper = new RecoveredTrx2UserInitiativeCountersMapper();

    @Test
    void test() {
        Counters expectedInvolvedDaily = new Counters(1L, 1_00L, 10_00L);
        Counters expectedInvolvedWeekly = new Counters(2L, 10_00L, 1_00L);
        Counters expectedInvolvedMonthly = new Counters(3L, 0L, 1_00L);
        Counters expectedInvolvedYearly = new Counters(4L, 10_00L, 0L);

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
        Counters expectedInvolvedDaily = new Counters(1L, 1_00L, 10_00L);
        Counters expectedInvolvedWeekly = new Counters(2L, 10_00L, 1_00L);
        Counters expectedInvolvedMonthly = new Counters(3L, 0L, 1_00L);
        Counters expectedInvolvedYearly = new Counters(4L, 10_00L, 0L);

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
            previous.setTotalRewardCents(1_00L);
            previous.setTotalAmountCents(10_00L);
            previous.setDailyCounters(buildTemporalCounter(dayKey));
            previous.setWeeklyCounters(buildTemporalCounter(weekKey));
            previous.setMonthlyCounters(buildTemporalCounter(monthKey));
            previous.setYearlyCounters(buildTemporalCounter(yearKey));
        }

        Reward reward = new Reward("INITIATIVEID", "ORGANIZATIONID",10_00L);
        reward.setCounters(new RewardCounters());
        reward.getCounters().setVersion((previous!=null?previous.getVersion(): 0L)+1);
        reward.getCounters().setExhaustedBudget(true);
        reward.getCounters().setTrxNumber((previous!=null?previous.getTrxNumber(): 0L) + 1);
        reward.getCounters().setTotalRewardCents((previous!=null?previous.getTotalRewardCents(): 0L) + 1_00L);
        reward.getCounters().setTotalAmountCents((previous!=null?previous.getTotalAmountCents(): 0L) + 1_00L);
        reward.getCounters().setDailyCounters(expectedInvolvedDaily!=null ? Map.of(dayKey, expectedInvolvedDaily) : null);
        reward.getCounters().setWeeklyCounters(expectedInvolvedWeekly!=null ? Map.of(weekKey, expectedInvolvedWeekly) : null);
        reward.getCounters().setMonthlyCounters(expectedInvolvedMonthly!=null ? Map.of(monthKey, expectedInvolvedMonthly) : null);
        reward.getCounters().setYearlyCounters(expectedInvolvedYearly!=null ? Map.of(yearKey, expectedInvolvedYearly) : null);

        TransactionProcessed trxStored = new TransactionProcessed();
        trxStored.setUserId("USERID");
        trxStored.setElaborationDateTime(LocalDateTime.now());

        // When
        UserInitiativeCounters result = mapper.apply(reward, trxStored, previous, InitiativeGeneralDTO.BeneficiaryTypeEnum.PF);

        // Then
        Assertions.assertNotNull(result);

        Assertions.assertSame(reward.getCounters().getVersion(), result.getVersion());

        Assertions.assertEquals(UserInitiativeCounters.buildId(trxStored.getUserId(), reward.getInitiativeId()), result.getId());
        Assertions.assertEquals(trxStored.getUserId(), result.getEntityId());
        Assertions.assertEquals(reward.getInitiativeId(), result.getInitiativeId());

        Assertions.assertSame(trxStored.getElaborationDateTime(), result.getUpdateDate());
        Assertions.assertSame(reward.getCounters().isExhaustedBudget(), result.isExhaustedBudget());

        Assertions.assertSame(reward.getCounters().getTrxNumber(), result.getTrxNumber());
        Assertions.assertSame(reward.getCounters().getTotalRewardCents(), result.getTotalRewardCents());
        Assertions.assertSame(reward.getCounters().getTotalAmountCents(), result.getTotalAmountCents());

        Assertions.assertEquals(buildExpectedTemporalCounter(reward.getCounters().getDailyCounters(), previous != null? previous.getDailyCounters(): new HashMap<>()), result.getDailyCounters());
        Assertions.assertEquals(buildExpectedTemporalCounter(reward.getCounters().getWeeklyCounters(), previous != null? previous.getWeeklyCounters(): new HashMap<>()), result.getWeeklyCounters());
        Assertions.assertEquals(buildExpectedTemporalCounter(reward.getCounters().getMonthlyCounters(), previous != null? previous.getMonthlyCounters(): new HashMap<>()), result.getMonthlyCounters());
        Assertions.assertEquals(buildExpectedTemporalCounter(reward.getCounters().getYearlyCounters(), previous != null? previous.getYearlyCounters(): new HashMap<>()), result.getYearlyCounters());

        TestUtils.checkNotNullFields(result, "pendingTrx");
    }

    private static Map<String, Counters> buildTemporalCounter(String key) {
        return Map.of("NOTINVOLEDKEY", new Counters(), key, new Counters(0L, 0L, 0L));
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
