package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

class RewardGroupsTrxCondition2DroolsConditionTransformerTest extends InitiativeTrxCondition2DroolsConditionTransformerTest {

    private final RewardGroupsTrxCondition2DroolsConditionTransformer transformer = new RewardGroupsTrxCondition2DroolsConditionTransformer();

    private final BigDecimal lowerBound = BigDecimal.ZERO;
    private final BigDecimal upperBound = BigDecimal.valueOf(10.37);

    @Test
    void testRewardGroupsGreaterThan(){
        RewardGroupsDTO initiativeTrxCondition = new RewardGroupsDTO();
        initiativeTrxCondition.setRewardGroups(List.of(
                RewardGroupsDTO.RewardGroupDTO.builder()
                        .from(lowerBound.subtract(BigDecimal.TEN))
                        .to(lowerBound.subtract(BigDecimal.ONE))
                        .build(),
                RewardGroupsDTO.RewardGroupDTO.builder()
                        .from(lowerBound)
                        .to(upperBound)
                        .build(),
                RewardGroupsDTO.RewardGroupDTO.builder()
                        .from(upperBound.add(BigDecimal.ONE))
                        .to(upperBound.add(BigDecimal.TEN))
                        .build()
        ));
        String RewardGroupsCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("((amount >= new java.math.BigDecimal(\"-10\") && amount <= new java.math.BigDecimal(\"-1\")) || (amount >= new java.math.BigDecimal(\"0\") && amount <= new java.math.BigDecimal(\"10.37\")) || (amount >= new java.math.BigDecimal(\"11.37\") && amount <= new java.math.BigDecimal(\"20.37\")))", RewardGroupsCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(RewardGroupsCondition, transaction);
        testUpperBound(RewardGroupsCondition, transaction);
    }

    private void testLowerBound(String RewardGroupsCondition, TransactionDroolsDTO transaction) {
        transaction.setAmount(bigDecimalValue(-0.01));
        testRule("RewardGroups", RewardGroupsCondition, transaction, false);

        transaction.setAmount(lowerBound.setScale(2, RoundingMode.UNNECESSARY));
        testRule("RewardGroups", RewardGroupsCondition, transaction, true);

        transaction.setAmount(bigDecimalValue(5.25));
        testRule("RewardGroups", RewardGroupsCondition, transaction, true);
    }

    private void testUpperBound(String RewardGroupsCondition, TransactionDroolsDTO transaction) {
        transaction.setAmount(bigDecimalValue(7.8));
        testRule("RewardGroups", RewardGroupsCondition, transaction, true);

        transaction.setAmount(upperBound.setScale(2, RoundingMode.UNNECESSARY));
        testRule("RewardGroups", RewardGroupsCondition, transaction, true);

        transaction.setAmount(bigDecimalValue(10.38));
        testRule("RewardGroups", RewardGroupsCondition, transaction, false);
    }

}
