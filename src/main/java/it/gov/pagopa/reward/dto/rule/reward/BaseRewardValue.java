package it.gov.pagopa.reward.dto.rule.reward;

import it.gov.pagopa.reward.enums.RewardValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class BaseRewardValue {
    private BigDecimal rewardValue;
    @Builder.Default
    private RewardValueType rewardValueType = RewardValueType.PERCENTAGE;

    public BaseRewardValue(BigDecimal rewardValue){
        this();
        this.rewardValue=rewardValue;
    }
}
