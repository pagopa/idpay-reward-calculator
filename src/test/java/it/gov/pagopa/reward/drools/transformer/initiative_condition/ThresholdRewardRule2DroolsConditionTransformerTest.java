package it.gov.pagopa.reward.drools.transformer.initiative_condition;

import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.model.RewardTransaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ThresholdRewardRule2DroolsConditionTransformerTest extends InitiativeRewardRule2DroolsConditionTransformerTest{

    private final ThresholdRewardRule2DroolsConditionTransformer transformer = new ThresholdRewardRule2DroolsConditionTransformer();

    private final BigDecimal lowerBound = BigDecimal.ZERO;
    private final BigDecimal upperBound = BigDecimal.valueOf(10.37);


    private void testLowerBound(String thresholdCondition, RewardTransaction transaction, boolean expectedBefore, boolean expectedEqual) {
        transaction.setAmount(bigDecimalValue(-0.01));
        testRule("Threshold", thresholdCondition, transaction, expectedBefore);

        transaction.setAmount(lowerBound.setScale(2, RoundingMode.UNNECESSARY));
        testRule("Threshold", thresholdCondition, transaction, expectedEqual);

        transaction.setAmount(bigDecimalValue(5.25));
        testRule("Threshold", thresholdCondition, transaction, true);
    }

    private void testUpperBound(String thresholdCondition, RewardTransaction transaction, boolean expectedEqual, boolean expectedGreater) {
        transaction.setAmount(bigDecimalValue(7.8));
        testRule("Threshold", thresholdCondition, transaction, true);

        transaction.setAmount(upperBound.setScale(2, RoundingMode.UNNECESSARY));
        testRule("Threshold", thresholdCondition, transaction, expectedEqual);

        transaction.setAmount(bigDecimalValue(10.38));
        testRule("Threshold", thresholdCondition, transaction, expectedGreater);
    }

    @Test
    public void testThresholdGreaterThan(){
        ThresholdDTO initiativeRewardRule = new ThresholdDTO();
        initiativeRewardRule.setFrom(lowerBound);
        initiativeRewardRule.setFromIncluded(false);
        String thresholdCondition = transformer.apply(initiativeRewardRule);

        Assertions.assertEquals("amount > new java.math.BigDecimal(\"0\")", thresholdCondition);

        RewardTransaction transaction = new RewardTransaction();

        testLowerBound(thresholdCondition, transaction, false, false);
        testUpperBound(thresholdCondition, transaction, true, true);
    }

    @Test
    public void testThresholdGreaterOrEqual(){
        ThresholdDTO initiativeRewardRule = new ThresholdDTO();
        initiativeRewardRule.setFrom(lowerBound);
        initiativeRewardRule.setFromIncluded(true);
        String thresholdCondition = transformer.apply(initiativeRewardRule);

        Assertions.assertEquals("amount >= new java.math.BigDecimal(\"0\")", thresholdCondition);

        RewardTransaction transaction = new RewardTransaction();

        testLowerBound(thresholdCondition, transaction, false, true);
        testUpperBound(thresholdCondition, transaction, true, true);
    }

    @Test
    public void testThresholdLowerThan(){
        ThresholdDTO initiativeRewardRule = new ThresholdDTO();
        initiativeRewardRule.setTo(upperBound);
        initiativeRewardRule.setToIncluded(false);
        String thresholdCondition = transformer.apply(initiativeRewardRule);

        Assertions.assertEquals("amount < new java.math.BigDecimal(\"10.37\")", thresholdCondition);

        RewardTransaction transaction = new RewardTransaction();

        testLowerBound(thresholdCondition, transaction, true, true);
        testUpperBound(thresholdCondition, transaction, false, false);
    }

    @Test
    public void testThresholdLowerOrEqual(){
        ThresholdDTO initiativeRewardRule = new ThresholdDTO();
        initiativeRewardRule.setTo(upperBound);
        initiativeRewardRule.setToIncluded(true);
        String thresholdCondition = transformer.apply(initiativeRewardRule);

        Assertions.assertEquals("amount <= new java.math.BigDecimal(\"10.37\")", thresholdCondition);

        RewardTransaction transaction = new RewardTransaction();

        testLowerBound(thresholdCondition, transaction, true, true);
        testUpperBound(thresholdCondition, transaction, true, false);
    }

}
