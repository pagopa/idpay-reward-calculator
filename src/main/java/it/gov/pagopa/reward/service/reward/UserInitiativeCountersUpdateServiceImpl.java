package it.gov.pagopa.reward.service.reward;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.Reward;
import it.gov.pagopa.reward.dto.RewardTransactionDTO;
import it.gov.pagopa.reward.model.counters.Counters;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import it.gov.pagopa.reward.model.counters.UserInitiativeCounters;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserInitiativeCountersUpdateServiceImpl implements UserInitiativeCountersUpdateService {

    private static final DateTimeFormatter dayDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter monthDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter yearDateFormatter = DateTimeFormatter.ofPattern("yyyy");

    private final DroolsContainerHolderService droolsContainerHolderService;


    public UserInitiativeCountersUpdateServiceImpl(DroolsContainerHolderService droolsContainerHolderService) {
        this.droolsContainerHolderService = droolsContainerHolderService;
    }


    @Override
    public void update(UserInitiativeCounters userInitiativeCounters, RewardTransactionDTO ruleEngineResult) {
        userInitiativeCounters.setUserId(userInitiativeCounters.getUserId());
        userInitiativeCounters.setInitiatives(userInitiativeCounters.getInitiatives());

        userInitiativeCounters.getInitiatives().forEach(o -> {
            // TODO use Constants
            if ((ruleEngineResult.getRewards().get(o.getInitiativeId()) != null && ruleEngineResult.getRewards().get(o.getInitiativeId()).getProvidedReward().compareTo(BigDecimal.ZERO) != 0) || List.of("TRX_RULE_TRXCOUNT_FAIL").equals(ruleEngineResult.getRejectionReason())) {
                updateCounters(o, ruleEngineResult.getRewards().get(o.getInitiativeId()), ruleEngineResult.getAmount());
                updateTemporalCounters(o, ruleEngineResult, droolsContainerHolderService.getInitiativeConfig(o.getInitiativeId()));
            }
        });

        // TODO operation of storno
    }

    private void updateTemporalCounters(InitiativeCounters initiativeCounters, RewardTransactionDTO ruleEngineResult, InitiativeConfig initiativeConfig) {

        if (initiativeConfig.isHasDailyThreshold()) {
            String dailyCountersKey = LocalDate.now().format(dayDateFormatter);
            updateCounters(initiativeCounters.getDailyCounters().get(dailyCountersKey), ruleEngineResult.getRewards().get(initiativeCounters.getInitiativeId()), ruleEngineResult.getAmount());
        }
        if (initiativeConfig.isHasMonthlyThreshold()) {
            String monthlyCountersKey = LocalDate.now().format(monthDateFormatter);
            updateCounters(initiativeCounters.getMonthlyCounters().get(monthlyCountersKey), ruleEngineResult.getRewards().get(initiativeCounters.getInitiativeId()), ruleEngineResult.getAmount());
        }
        if (initiativeConfig.isHasYearlyThreshold()) {
            String yearlyCountersKey = LocalDate.now().format(yearDateFormatter);
            updateCounters(initiativeCounters.getYearlyCounters().get(yearlyCountersKey), ruleEngineResult.getRewards().get(initiativeCounters.getInitiativeId()), ruleEngineResult.getAmount());
        }
    }

    private void updateCounters(Counters counters, Reward reward, BigDecimal amount) {
        counters.setTrxNumber(counters.getTrxNumber() + 1);
        counters.setTotalReward(counters.getTotalReward().add(reward.getProvidedReward()));
        counters.setTotalAmount(counters.getTotalAmount().add(amount));
    }
}
