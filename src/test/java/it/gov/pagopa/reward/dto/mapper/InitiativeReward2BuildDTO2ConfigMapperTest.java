package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class InitiativeReward2BuildDTO2ConfigMapperTest {

    @Test
    void apply() {
        // Given
        InitiativeReward2BuildDTO initiative = InitiativeReward2BuildDTOFaker.mockInstance(1);

        InitiativeReward2BuildDTO2ConfigMapper initiativeReward2BuildDTO2ConfigMapper = new InitiativeReward2BuildDTO2ConfigMapper();

        // When
        InitiativeConfig result = initiativeReward2BuildDTO2ConfigMapper.apply(initiative);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertTrue(result.isHasDailyThreshold());
        Assertions.assertFalse(result.isHasWeeklyThreshold());
        Assertions.assertFalse(result.isHasMonthlyThreshold());
        Assertions.assertFalse(result.isHasYearlyThreshold());

    }

    @Test
    void applyAllFrequencyTrue() {
        // Given
        InitiativeReward2BuildDTO initiative = new InitiativeReward2BuildDTO();
        initiative.setInitiativeId("INITIATIVE_ID");
        initiative.setTrxRule(new InitiativeTrxConditions());

        initiative.getTrxRule().setRewardLimits(List.of(
                RewardLimitsDTO.builder().frequency(RewardLimitsDTO.RewardLimitFrequency.DAILY).build(),
                RewardLimitsDTO.builder().frequency(RewardLimitsDTO.RewardLimitFrequency.WEEKLY).build(),
                RewardLimitsDTO.builder().frequency(RewardLimitsDTO.RewardLimitFrequency.MONTHLY).build(),
                RewardLimitsDTO.builder().frequency(RewardLimitsDTO.RewardLimitFrequency.YEARLY).build()
        ));

        InitiativeReward2BuildDTO2ConfigMapper initiativeReward2BuildDTO2ConfigMapper = new InitiativeReward2BuildDTO2ConfigMapper();

        // When
        InitiativeConfig result = initiativeReward2BuildDTO2ConfigMapper.apply(initiative);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertTrue(result.isHasDailyThreshold());
        Assertions.assertTrue(result.isHasWeeklyThreshold());
        Assertions.assertTrue(result.isHasMonthlyThreshold());
        Assertions.assertTrue(result.isHasYearlyThreshold());

    }
}