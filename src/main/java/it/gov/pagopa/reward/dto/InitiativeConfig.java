package it.gov.pagopa.reward.dto;

import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.enums.InitiativeRewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InitiativeConfig {

    private String initiativeId;
    private String initiativeName;
    private String organizationId;
    private Long beneficiaryBudgetCents;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean dailyThreshold;
    private boolean weeklyThreshold;
    private boolean monthlyThreshold;
    private boolean yearlyThreshold;
    private InitiativeRewardRule rewardRule;
    private InitiativeTrxConditions trxRule;
    private InitiativeRewardType initiativeRewardType;
    private InitiativeGeneralDTO.BeneficiaryTypeEnum beneficiaryType;

}
