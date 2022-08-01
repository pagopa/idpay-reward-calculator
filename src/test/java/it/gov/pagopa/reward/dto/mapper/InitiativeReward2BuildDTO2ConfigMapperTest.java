package it.gov.pagopa.reward.dto.mapper;

import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

class InitiativeReward2BuildDTO2ConfigMapperTest {

    @Test
    void mapperDailyFrequencyType() {
        // Given
        InitiativeReward2BuildDTO initiative = InitiativeReward2BuildDTOFaker.mockInstanceWithFrequencyType(
                1,
                RewardLimitsDTO.RewardLimitFrequency.DAILY);

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


        TestUtils.checkNotNullFields(result);
    }

    @Test
    void mapperWeeklyFrequencyType() {
        // Given
        InitiativeReward2BuildDTO initiative = InitiativeReward2BuildDTOFaker.mockInstanceWithFrequencyType(
                1,
                RewardLimitsDTO.RewardLimitFrequency.WEEKLY);

        InitiativeReward2BuildDTO2ConfigMapper initiativeReward2BuildDTO2ConfigMapper = new InitiativeReward2BuildDTO2ConfigMapper();

        // When
        InitiativeConfig result = initiativeReward2BuildDTO2ConfigMapper.apply(initiative);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertFalse(result.isHasDailyThreshold());
        Assertions.assertTrue(result.isHasWeeklyThreshold());
        Assertions.assertFalse(result.isHasMonthlyThreshold());
        Assertions.assertFalse(result.isHasYearlyThreshold());

    }

    @Test
    void mapperMonthlyFrequencyType() {
        // Given
        InitiativeReward2BuildDTO initiative = InitiativeReward2BuildDTOFaker.mockInstanceWithFrequencyType(
                1,
                RewardLimitsDTO.RewardLimitFrequency.MONTHLY);

        InitiativeReward2BuildDTO2ConfigMapper initiativeReward2BuildDTO2ConfigMapper = new InitiativeReward2BuildDTO2ConfigMapper();

        // When
        InitiativeConfig result = initiativeReward2BuildDTO2ConfigMapper.apply(initiative);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertFalse(result.isHasDailyThreshold());
        Assertions.assertFalse(result.isHasWeeklyThreshold());
        Assertions.assertTrue(result.isHasMonthlyThreshold());
        Assertions.assertFalse(result.isHasYearlyThreshold());

    }

    @Test
    void mapperYearlyFrequencyType() {
        // Given
        InitiativeReward2BuildDTO initiative = InitiativeReward2BuildDTOFaker.mockInstanceWithFrequencyType(
                1,
                RewardLimitsDTO.RewardLimitFrequency.YEARLY);

        InitiativeReward2BuildDTO2ConfigMapper initiativeReward2BuildDTO2ConfigMapper = new InitiativeReward2BuildDTO2ConfigMapper();

        // When
        InitiativeConfig result = initiativeReward2BuildDTO2ConfigMapper.apply(initiative);

        // Then
        Assertions.assertNotNull(result);
        TestUtils.checkNotNullFields(result);
        Assertions.assertFalse(result.isHasDailyThreshold());
        Assertions.assertFalse(result.isHasWeeklyThreshold());
        Assertions.assertFalse(result.isHasMonthlyThreshold());
        Assertions.assertTrue(result.isHasYearlyThreshold());

    }
    @Test
    void mapperAllFrequencyType() {
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

    @Test
    void mapperIllegalFrequencyTypeNull() {
        // Given
        InitiativeReward2BuildDTO initiative = new InitiativeReward2BuildDTO();
        initiative.setInitiativeId("INITIATIVE_ID");
        initiative.setTrxRule(new InitiativeTrxConditions());

        initiative.getTrxRule().setRewardLimits(List.of(
                RewardLimitsDTO.builder().frequency(null)
                        .rewardLimit(new BigDecimal("100.00")).build()
        ));

        InitiativeReward2BuildDTO2ConfigMapper initiativeReward2BuildDTO2ConfigMapper = new InitiativeReward2BuildDTO2ConfigMapper();

        // When
        try {
            InitiativeConfig result = initiativeReward2BuildDTO2ConfigMapper.apply(initiative);
        }catch (IllegalArgumentException actualException){
            Assertions.assertEquals("Frequency cannot be null",actualException.getMessage());
        }
    }
}