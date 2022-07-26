package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class UserInitiativeCountersUpdateServiceImpl implements UserInitiativeCountersUpdateService{

    private final DroolsContainerHolderService droolsContainerHolderService;

    public UserInitiativeCountersUpdateServiceImpl(DroolsContainerHolderService droolsContainerHolderService) {
        this.droolsContainerHolderService = droolsContainerHolderService;
    }

    @Override
    public void update(RewardTransactionDTO ruleEngineResult, UserInitiativeCounters userInitiativeCounters) {
        UserInitiativeCounters out = new UserInitiativeCounters();
        out.setUserId(userInitiativeCounters.getUserId());
        out.setInitiatives(userInitiativeCounters.getInitiatives());

        out.getInitiatives().forEach(o -> {
            updateCounters(o, ruleEngineResult, o.getInitiativeId());
            updateThresholdCounters(o, ruleEngineResult, droolsContainerHolderService.getInitiativeConfig(o.getInitiativeId()));
        });
    }

    private void updateThresholdCounters(InitiativeCounters initiativeCounters, RewardTransactionDTO ruleEngineResult, InitiativeConfig initiativeConfig) {

        String dayFormattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String monthFormattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String yearFormattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));

        if(initiativeConfig.isHasDailyThreshold()) {
            updateCounters(initiativeCounters.getDailyCounters().get(dayFormattedDate), ruleEngineResult, initiativeCounters.getInitiativeId());
        }
        if(initiativeConfig.isHasMonthlyThreshold()) {
            updateCounters(initiativeCounters.getMonthlyCounters().get(monthFormattedDate), ruleEngineResult, initiativeCounters.getInitiativeId());
        }
        if(initiativeConfig.isHasDailyThreshold()) {
            updateCounters(initiativeCounters.getYearlyCounters().get(yearFormattedDate), ruleEngineResult, initiativeCounters.getInitiativeId());
        }
    }

    private void updateCounters(Counters counters, RewardTransactionDTO ruleEngineResult, String initiativeId){
        counters.setTrxNumber(counters.getTrxNumber() + 1);
        counters.setTotalReward(counters.getTotalReward().add(ruleEngineResult.getRewards().get(initiativeId)));
        counters.setTotalAmount(ruleEngineResult.getAmount());
    }
}
