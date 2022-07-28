package it.gov.pagopa.reward.dto.rule.reward;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RewardValueDTO implements InitiativeRewardRule {
    private BigDecimal rewardValue;
}
