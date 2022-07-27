package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
public class InitiativeReward2BuildDTO2ConfigMapper implements Function<InitiativeReward2BuildDTO, InitiativeConfig> {
    @Override
    public InitiativeConfig apply(InitiativeReward2BuildDTO initiativeReward2BuildDTO) {
        InitiativeConfig out = new InitiativeConfig();
        out.setInitiativeId(initiativeReward2BuildDTO.getInitiativeId());
        List<RewardLimitsDTO> rewardLimits = initiativeReward2BuildDTO.getTrxRule().getRewardLimits();
        rewardLimits.forEach(l -> setLimitFrequency(l.getFrequency(),out));
        return out;
    }

    void setLimitFrequency(RewardLimitsDTO.RewardLimitFrequency limitFrequency, InitiativeConfig initiativeConfig){
        if(limitFrequency!=null) {
            switch (limitFrequency) {
                case DAILY -> initiativeConfig.setHasDailyThreshold(true);
                case WEEKLY -> initiativeConfig.setHasWeeklyThreshold(true);
                case MONTHLY -> initiativeConfig.setHasMonthlyThreshold(true);
                case YEARLY -> initiativeConfig.setHasYearlyThreshold(true);
                default -> throw new IllegalArgumentException("Frequency type not expected");
            }
        }else {
            throw new IllegalArgumentException("Frequency cannot be null");
        }
    }
}
