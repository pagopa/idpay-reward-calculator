package it.gov.pagopa.reward.dto.rule.trx;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class RewardLimitsDTO implements InitiativeTrxCondition {
    private RewardLimitFrequency frequency;

    private BigDecimal rewardLimit;

    public enum RewardLimitFrequency {
        DAILY,
        MONTHLY,
        YEARLY
    }
}
