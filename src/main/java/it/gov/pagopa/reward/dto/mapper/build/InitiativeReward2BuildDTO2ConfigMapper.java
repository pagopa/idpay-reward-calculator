package it.gov.pagopa.reward.dto.mapper.build;

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
        out.setInitiativeName(initiativeReward2BuildDTO.getInitiativeName());
        out.setOrganizationId(initiativeReward2BuildDTO.getOrganizationId());
        out.setBeneficiaryBudgetCents(initiativeReward2BuildDTO.getGeneral()!=null?initiativeReward2BuildDTO.getGeneral().getBeneficiaryBudgetCents():null);
        out.setStartDate(initiativeReward2BuildDTO.getGeneral()!=null?initiativeReward2BuildDTO.getGeneral().getStartDate():null);
        out.setEndDate(initiativeReward2BuildDTO.getGeneral()!=null?initiativeReward2BuildDTO.getGeneral().getEndDate():null);
        out.setTrxRule(initiativeReward2BuildDTO.getTrxRule());
        out.setRewardRule(initiativeReward2BuildDTO.getRewardRule());
        out.setInitiativeRewardType(initiativeReward2BuildDTO.getInitiativeRewardType());
        out.setBeneficiaryType(initiativeReward2BuildDTO.getGeneral().getBeneficiaryType());
        setPeriodicalInfo(initiativeReward2BuildDTO, out);
        return out;
    }

    private void setPeriodicalInfo(InitiativeReward2BuildDTO initiativeReward2BuildDTO, InitiativeConfig out) {
        List<RewardLimitsDTO> rewardLimits = initiativeReward2BuildDTO.getTrxRule().getRewardLimits();
        if(rewardLimits!=null){
            rewardLimits.forEach(l -> setLimitFrequency(l.getFrequency(), out));
        }
    }

    private void setLimitFrequency(RewardLimitsDTO.RewardLimitFrequency limitFrequency, InitiativeConfig initiativeConfig){
        if(limitFrequency!=null) {
            switch (limitFrequency) {
                case DAILY -> initiativeConfig.setDailyThreshold(true);
                case WEEKLY -> initiativeConfig.setWeeklyThreshold(true);
                case MONTHLY -> initiativeConfig.setMonthlyThreshold(true);
                case YEARLY -> initiativeConfig.setYearlyThreshold(true);
                default -> throw new IllegalArgumentException("Frequency type not expected");
            }
        }else {
            throw new IllegalArgumentException("Frequency cannot be null");
        }
    }
}
