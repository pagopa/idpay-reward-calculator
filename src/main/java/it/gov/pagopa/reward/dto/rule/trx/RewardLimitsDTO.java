package it.gov.pagopa.reward.dto.rule.trx;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class RewardLimitsDTO implements InitiativeTrxCondition, InitiativeTrxConsequence {
    private RewardLimitFrequency frequency;

    private BigDecimal rewardLimit;

    public enum RewardLimitFrequency {
        DAILY,
        MONTHLY,
        WEEKLY,
        YEARLY
    }
}
