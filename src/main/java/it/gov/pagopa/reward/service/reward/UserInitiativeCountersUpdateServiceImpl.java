package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class UserInitiativeCountersUpdateServiceImpl implements UserInitiativeCountersUpdateService{

    @Override
    public void update(RewardTransactionDTO ruleEngineResult, UserInitiativeCounters userInitiativeCounters) {
        UserInitiativeCounters out = new UserInitiativeCounters();
        out.setUserId(userInitiativeCounters.getUserId());
        out.setInitiatives(userInitiativeCounters.getInitiatives());

        out.getInitiatives().forEach(o -> {
            o.setTrxNumber(o.getTrxNumber() + 1);
            o.setTotalReward(o.getTotalReward().add(ruleEngineResult.getRewards().get(o.getInitiativeId())));
            o.setTotalAmount(ruleEngineResult.getAmount());
        });

        // TODO update daily, monthly, yearly counters according to getInitiativeConfig boolean
    }
}
