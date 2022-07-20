package it.gov.pagopa.reward.dto.build;

import it.gov.pagopa.reward.dto.rule.reward.AnyOfInitiativeRewardRule;
import lombok.Data;

import java.util.List;

@Data
public class InitiativeReward2BuildDTO {
    private String initiativeId;
    private String initiativeName;
    private List<AnyOfInitiativeRewardRule> rewardRule;
}
