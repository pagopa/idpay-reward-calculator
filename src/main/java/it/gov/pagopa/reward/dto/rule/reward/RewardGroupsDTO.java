package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxCondition;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RewardGroupsDTO implements InitiativeRewardRule, InitiativeTrxCondition {
    private List<RewardGroupDTO> rewardGroups;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder
    @EqualsAndHashCode(callSuper = true)
    @JsonPropertyOrder({
            "from",
            "to",
            "rewardValue",
            "rewardValueType"
    })
    @ToString(callSuper = true)
    public static class RewardGroupDTO extends BaseRewardValue {
        private BigDecimal from;
        private BigDecimal to;

        public RewardGroupDTO(BigDecimal from, BigDecimal to, BigDecimal rewardValue){
            super(rewardValue);
            this.from=from;
            this.to=to;
        }
    }
}
