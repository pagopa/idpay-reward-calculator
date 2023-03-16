package it.gov.pagopa.reward.dto.rule.reward;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RewardValueDTO extends BaseRewardValue implements InitiativeRewardRule {
    public RewardValueDTO(BigDecimal rewardValue){
        super(rewardValue);
    }
}
