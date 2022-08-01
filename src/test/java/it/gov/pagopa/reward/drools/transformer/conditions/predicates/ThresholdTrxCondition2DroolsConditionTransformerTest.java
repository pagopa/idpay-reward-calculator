package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

class ThresholdTrxCondition2DroolsConditionTransformerTest extends InitiativeTrxCondition2DroolsConditionTransformerTest {

    private final ThresholdTrxCondition2DroolsConditionTransformer transformer = new ThresholdTrxCondition2DroolsConditionTransformer();

    private final BigDecimal lowerBound = BigDecimal.ZERO;
    private final BigDecimal upperBound = BigDecimal.valueOf(10.37);


    private void testLowerBound(String thresholdCondition, TransactionDroolsDTO transaction, boolean expectedBefore, boolean expectedEqual) {
        transaction.setAmount(bigDecimalValue(-0.01));
        testRule("Threshold", thresholdCondition, transaction, expectedBefore);

        transaction.setAmount(lowerBound.setScale(2, RoundingMode.UNNECESSARY));
        testRule("Threshold", thresholdCondition, transaction, expectedEqual);

        transaction.setAmount(bigDecimalValue(5.25));
        testRule("Threshold", thresholdCondition, transaction, true);
    }

    private void testUpperBound(String thresholdCondition, TransactionDroolsDTO transaction, boolean expectedEqual, boolean expectedGreater) {
        transaction.setAmount(bigDecimalValue(7.8));
        testRule("Threshold", thresholdCondition, transaction, true);

        transaction.setAmount(upperBound.setScale(2, RoundingMode.UNNECESSARY));
        testRule("Threshold", thresholdCondition, transaction, expectedEqual);

        transaction.setAmount(bigDecimalValue(10.38));
        testRule("Threshold", thresholdCondition, transaction, expectedGreater);
    }

    @Test
    void testThresholdGreaterThan(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setFrom(lowerBound);
        initiativeTrxCondition.setFromIncluded(false);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("amount > new java.math.BigDecimal(\"0\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, false);
        testUpperBound(thresholdCondition, transaction, true, true);
    }

    @Test
    void testThresholdGreaterOrEqual(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setFrom(lowerBound);
        initiativeTrxCondition.setFromIncluded(true);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("amount >= new java.math.BigDecimal(\"0\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, true);
        testUpperBound(thresholdCondition, transaction, true, true);
    }

    @Test
    void testThresholdLowerThan(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setTo(upperBound);
        initiativeTrxCondition.setToIncluded(false);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("amount < new java.math.BigDecimal(\"10.37\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, true, true);
        testUpperBound(thresholdCondition, transaction, false, false);
    }

    @Test
    void testThresholdLowerOrEqual(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setTo(upperBound);
        initiativeTrxCondition.setToIncluded(true);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("amount <= new java.math.BigDecimal(\"10.37\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, true, true);
        testUpperBound(thresholdCondition, transaction, true, false);
    }

    @Test
    void testThresholdBetweenClosed(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setFrom(lowerBound);
        initiativeTrxCondition.setFromIncluded(true);
        initiativeTrxCondition.setTo(upperBound);
        initiativeTrxCondition.setToIncluded(true);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("amount >= new java.math.BigDecimal(\"0\") && amount <= new java.math.BigDecimal(\"10.37\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, true);
        testUpperBound(thresholdCondition, transaction, true, false);
    }

    @Test
    void testThresholdBetweenOpen(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setFrom(lowerBound);
        initiativeTrxCondition.setFromIncluded(false);
        initiativeTrxCondition.setTo(upperBound);
        initiativeTrxCondition.setToIncluded(false);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("amount > new java.math.BigDecimal(\"0\") && amount < new java.math.BigDecimal(\"10.37\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, false);
        testUpperBound(thresholdCondition, transaction, false, false);
    }

}
