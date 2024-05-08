package it.gov.pagopa.reward.dto.rule.trx;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class RewardLimitsDTO implements InitiativeTrxCondition, InitiativeTrxConsequence {
    private RewardLimitFrequency frequency;

    private Long rewardLimitCents;

    public enum RewardLimitFrequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }
}
