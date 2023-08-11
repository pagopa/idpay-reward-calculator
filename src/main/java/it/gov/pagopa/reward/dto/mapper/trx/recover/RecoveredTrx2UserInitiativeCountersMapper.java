package it.gov.pagopa.reward.dto.mapper.trx.recover;

import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.trx.Reward;
import it.gov.pagopa.reward.model.TransactionProcessed;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RecoveredTrx2UserInitiativeCountersMapper {

    public UserInitiativeCounters apply(Reward r, TransactionProcessed trxStored, UserInitiativeCounters previous, InitiativeGeneralDTO.BeneficiaryTypeEnum entityType) {
        return UserInitiativeCounters.builder(trxStored.getUserId(), entityType, r.getInitiativeId())
                .version(r.getCounters().getVersion())
                .updateDate(trxStored.getElaborationDateTime())
                .exhaustedBudget(r.getCounters().isExhaustedBudget())
                .trxNumber(r.getCounters().getTrxNumber())
                .totalReward(r.getCounters().getTotalReward())
                .totalAmount(r.getCounters().getTotalAmount())

                .dailyCounters(applyDelta(previous != null? previous.getDailyCounters() : new HashMap<>(), r.getCounters().getDailyCounters()))
                .weeklyCounters(applyDelta(previous != null? previous.getWeeklyCounters() : new HashMap<>(), r.getCounters().getWeeklyCounters()))
                .monthlyCounters(applyDelta(previous != null? previous.getMonthlyCounters() : new HashMap<>(), r.getCounters().getMonthlyCounters()))
                .yearlyCounters(applyDelta(previous != null? previous.getYearlyCounters() : new HashMap<>(), r.getCounters().getYearlyCounters()))

                .build();
    }

    private Map<String, Counters> applyDelta(Map<String, Counters> previousCounters, Map<String, Counters> delta) {
        if(delta!=null){
            HashMap<String, Counters> updated = new HashMap<>(previousCounters);
            updated.putAll(delta);
            return updated;
        } else {
            return previousCounters;
        }
    }
}
