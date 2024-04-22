package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.trx.ThresholdDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ThresholdTrxCondition2DroolsConditionTransformerTest extends InitiativeTrxCondition2DroolsConditionTransformerTest {

    private final String initiativeId = "Threshold";
    private final ThresholdTrxCondition2DroolsConditionTransformer transformer = new ThresholdTrxCondition2DroolsConditionTransformer();

    private final Long lowerBound = 0L;
    private final Long upperBound = 10_37L;


    private void testLowerBound(String thresholdCondition, TransactionDroolsDTO transaction, boolean expectedBefore, boolean expectedEqual) {
        transaction.setEffectiveAmountCents(-1L);
        testRule(initiativeId, thresholdCondition, transaction, expectedBefore);

        transaction.setEffectiveAmountCents(lowerBound);
        testRule(initiativeId, thresholdCondition, transaction, expectedEqual);

        transaction.setEffectiveAmountCents(5_25L);
        testRule(initiativeId, thresholdCondition, transaction, true);
    }

    private void testUpperBound(String thresholdCondition, TransactionDroolsDTO transaction, boolean expectedEqual, boolean expectedGreater) {
        transaction.setEffectiveAmountCents(7_80L);
        testRule(initiativeId, thresholdCondition, transaction, true);

        transaction.setEffectiveAmountCents(upperBound);
        testRule(initiativeId, thresholdCondition, transaction, expectedEqual);

        transaction.setEffectiveAmountCents(10_38L);
        testRule(initiativeId, thresholdCondition, transaction, expectedGreater);
    }

    @Test
    void testThresholdGreaterThan(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setFromCents(lowerBound);
        initiativeTrxCondition.setFromIncluded(false);
        String thresholdCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("effectiveAmountCents > new java.lang.Long(\"0\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, false);
        testUpperBound(thresholdCondition, transaction, true, true);
    }

    @Test
    void testThresholdGreaterOrEqual(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setFromCents(lowerBound);
        initiativeTrxCondition.setFromIncluded(true);
        String thresholdCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("effectiveAmountCents >= new java.lang.Long(\"0\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, true);
        testUpperBound(thresholdCondition, transaction, true, true);
    }

    @Test
    void testThresholdLowerThan(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setToCents(upperBound);
        initiativeTrxCondition.setToIncluded(false);
        String thresholdCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("effectiveAmountCents < new java.lang.Long(\"1037\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, true, true);
        testUpperBound(thresholdCondition, transaction, false, false);
    }

    @Test
    void testThresholdLowerOrEqual(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setToCents(upperBound);
        initiativeTrxCondition.setToIncluded(true);
        String thresholdCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("effectiveAmountCents <= new java.lang.Long(\"1037\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, true, true);
        testUpperBound(thresholdCondition, transaction, true, false);
    }

    @Test
    void testThresholdBetweenClosed(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setFromCents(lowerBound);
        initiativeTrxCondition.setFromIncluded(true);
        initiativeTrxCondition.setToCents(upperBound);
        initiativeTrxCondition.setToIncluded(true);
        String thresholdCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("effectiveAmountCents >= new java.lang.Long(\"0\") && effectiveAmountCents <= new java.lang.Long(\"1037\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, true);
        testUpperBound(thresholdCondition, transaction, true, false);
    }

    @Test
    void testThresholdBetweenOpen(){
        ThresholdDTO initiativeTrxCondition = new ThresholdDTO();
        initiativeTrxCondition.setFromCents(lowerBound);
        initiativeTrxCondition.setFromIncluded(false);
        initiativeTrxCondition.setToCents(upperBound);
        initiativeTrxCondition.setToIncluded(false);
        String thresholdCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("effectiveAmountCents > new java.lang.Long(\"0\") && effectiveAmountCents < new java.lang.Long(\"1037\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, false);
        testUpperBound(thresholdCondition, transaction, false, false);
    }

}
