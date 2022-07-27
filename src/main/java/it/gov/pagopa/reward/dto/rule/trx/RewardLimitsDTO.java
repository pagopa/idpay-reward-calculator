package it.gov.pagopa.reward.dto.rule.trx;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class RewardLimitsDTO implements InitiativeTrxCondition, InitiativeRewardRule {
    private RewardLimitFrequency frequency;

    private BigDecimal rewardLimit;

    public enum RewardLimitFrequency {
        DAILY,
        MONTHLY,
        WEEKLY,
        YEARLY
    }
}
