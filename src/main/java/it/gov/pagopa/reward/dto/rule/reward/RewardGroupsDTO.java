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
            "fromCents",
            "toCents",
            "rewardValue",
            "rewardValueType"
    })
    @ToString(callSuper = true)
    public static class RewardGroupDTO extends BaseRewardValue {
        private Long fromCents;
        private Long toCents;

        public RewardGroupDTO(Long fromCents, Long toCents, BigDecimal rewardValue){
            super(rewardValue);
            this.fromCents=fromCents;
            this.toCents=toCents;
        }
    }
}
