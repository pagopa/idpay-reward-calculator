package it.gov.pagopa.reward.dto;

import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InitiativeConfig {

    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private BigDecimal beneficiaryBudget;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean dailyThreshold;
    private boolean weeklyThreshold;
    private boolean monthlyThreshold;
    private boolean yearlyThreshold;
    private InitiativeRewardRule rewardRule;
    private InitiativeTrxConditions trxRule;
    private String initiativeRewardType;

}
