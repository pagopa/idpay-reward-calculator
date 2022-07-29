package it.gov.pagopa.reward.dto.build;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class InitiativeReward2BuildDTO {
    private String initiativeId;
    private String initiativeName;
    private InitiativeRewardRule rewardRule;
    private InitiativeTrxConditions trxRule;
}
