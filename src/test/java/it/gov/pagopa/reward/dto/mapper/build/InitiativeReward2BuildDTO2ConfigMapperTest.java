package it.gov.pagopa.reward.dto.mapper.build;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.reward.dto.InitiativeConfig;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.test.fakers.InitiativeReward2BuildDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

class InitiativeReward2BuildDTO2ConfigMapperTest {
    @Test
    void test() {
        // Given
        InitiativeReward2BuildDTO initiative = InitiativeReward2BuildDTOFaker.mockInstanceWithFrequencyType(
                1,
                RewardLimitsDTO.RewardLimitFrequency.DAILY);

        InitiativeReward2BuildDTO2ConfigMapper initiativeReward2BuildDTO2ConfigMapper = new InitiativeReward2BuildDTO2ConfigMapper();

        // When
        InitiativeConfig result = initiativeReward2BuildDTO2ConfigMapper.apply(initiative);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(initiative.getInitiativeId(), result.getInitiativeId());
        Assertions.assertEquals(initiative.getInitiativeName(), result.getInitiativeName());
        Assertions.assertEquals(initiative.getOrganizationId(), result.getOrganizationId());
        Assertions.assertEquals(initiative.getGeneral().getStartDate(), result.getStartDate());
        Assertions.assertEquals(initiative.getGeneral().getEndDate(), result.getEndDate());
        Assertions.assertEquals(initiative.getGeneral().getBeneficiaryBudget(), result.getBeneficiaryBudget());
        Assertions.assertSame(initiative.getTrxRule(), result.getTrxRule());
        Assertions.assertSame(initiative.getRewardRule(), result.getRewardRule());
        Assertions.assertSame(initiative.getInitiativeRewardType(), result.getInitiativeRewardType());

        TestUtils.checkNotNullFields(result);

    }

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
        Assertions.assertTrue(result.isDailyThreshold());
        Assertions.assertFalse(result.isWeeklyThreshold());
        Assertions.assertFalse(result.isMonthlyThreshold());
        Assertions.assertFalse(result.isYearlyThreshold());


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
        Assertions.assertFalse(result.isDailyThreshold());
        Assertions.assertTrue(result.isWeeklyThreshold());
        Assertions.assertFalse(result.isMonthlyThreshold());
        Assertions.assertFalse(result.isYearlyThreshold());

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
        Assertions.assertFalse(result.isDailyThreshold());
        Assertions.assertFalse(result.isWeeklyThreshold());
        Assertions.assertTrue(result.isMonthlyThreshold());
        Assertions.assertFalse(result.isYearlyThreshold());

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
        Assertions.assertFalse(result.isDailyThreshold());
        Assertions.assertFalse(result.isWeeklyThreshold());
        Assertions.assertFalse(result.isMonthlyThreshold());
        Assertions.assertTrue(result.isYearlyThreshold());

    }
    @Test
    void mapperAllFrequencyType() {
        // Given
        InitiativeReward2BuildDTO initiative = InitiativeReward2BuildDTOFaker.mockInstance(1);
        initiative.getGeneral().setStartDate(LocalDate.MIN);
        initiative.getGeneral().setEndDate(LocalDate.MAX);
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
        Assertions.assertTrue(result.isDailyThreshold());
        Assertions.assertTrue(result.isWeeklyThreshold());
        Assertions.assertTrue(result.isMonthlyThreshold());
        Assertions.assertTrue(result.isYearlyThreshold());

    }

    @Test
    void mapperIllegalFrequencyTypeNull() {
        // Given
        InitiativeReward2BuildDTO initiative = new InitiativeReward2BuildDTO();
        initiative.setInitiativeId("INITIATIVE_ID");

        InitiativeGeneralDTO initiativeGeneralDTO = new InitiativeGeneralDTO();
        initiative.setGeneral(initiativeGeneralDTO);

        initiative.setTrxRule(new InitiativeTrxConditions());

        initiative.getTrxRule().setRewardLimits(List.of(
                RewardLimitsDTO.builder().frequency(null)
                        .rewardLimit(new BigDecimal("100.00")).build()
        ));

        InitiativeReward2BuildDTO2ConfigMapper initiativeReward2BuildDTO2ConfigMapper = new InitiativeReward2BuildDTO2ConfigMapper();

        // When
        try {
            initiativeReward2BuildDTO2ConfigMapper.apply(initiative);
            Assertions.fail("Expected exception");
        }catch (IllegalArgumentException actualException){
            Assertions.assertEquals("Frequency cannot be null",actualException.getMessage());
        }
    }
}