package it.gov.pagopa.reward.dto.rule.reward;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RewardGroupsDTO implements InitiativeRewardRule {
    @JsonProperty("rewardGroups")
    private List<RewardGroupDTO> rewardGroupDTOS;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RewardGroupDTO {
        private BigDecimal from;
        private BigDecimal to;
        private BigDecimal rewardValue;
    }
}
