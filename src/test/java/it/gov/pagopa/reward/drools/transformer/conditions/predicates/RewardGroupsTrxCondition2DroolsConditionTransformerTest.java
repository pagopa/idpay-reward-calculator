package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class RewardGroupsTrxCondition2DroolsConditionTransformerTest extends InitiativeTrxCondition2DroolsConditionTransformerTest {

    private final String initiativeId = "RewardGroups";
    private final RewardGroupsTrxCondition2DroolsConditionTransformer transformer = new RewardGroupsTrxCondition2DroolsConditionTransformer();

    private final Long lowerBound = 0L;
    private final Long upperBound = 10_37L;

    @Test
    void testRewardGroupsGreaterThan(){
        RewardGroupsDTO initiativeTrxCondition = new RewardGroupsDTO();
        initiativeTrxCondition.setRewardGroups(List.of(
                RewardGroupsDTO.RewardGroupDTO.builder()
                        .fromCents(lowerBound - 10_00)
                        .toCents(lowerBound - 1_00)
                        .build(),
                RewardGroupsDTO.RewardGroupDTO.builder()
                        .fromCents(lowerBound)
                        .toCents(upperBound)
                        .build(),
                RewardGroupsDTO.RewardGroupDTO.builder()
                        .fromCents(upperBound + 1_00L)
                        .toCents(upperBound + 10_00L)
                        .build()
        ));
        String RewardGroupsCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("((effectiveAmountCents >= new java.lang.Long(\"-1000\") && effectiveAmountCents <= new java.lang.Long(\"-100\")) || (effectiveAmountCents >= new java.lang.Long(\"0\") && effectiveAmountCents <= new java.lang.Long(\"1037\")) || (effectiveAmountCents >= new java.lang.Long(\"1137\") && effectiveAmountCents <= new java.lang.Long(\"2037\")))", RewardGroupsCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(RewardGroupsCondition, transaction);
        testUpperBound(RewardGroupsCondition, transaction);
    }

    private void testLowerBound(String RewardGroupsCondition, TransactionDroolsDTO transaction) {
        transaction.setEffectiveAmountCents(-1L);
        testRule(initiativeId, RewardGroupsCondition, transaction, false);

        transaction.setEffectiveAmountCents(lowerBound);
        testRule(initiativeId, RewardGroupsCondition, transaction, true);

        transaction.setEffectiveAmountCents(525L);
        testRule(initiativeId, RewardGroupsCondition, transaction, true);
    }

    private void testUpperBound(String RewardGroupsCondition, TransactionDroolsDTO transaction) {
        transaction.setEffectiveAmountCents(780L);
        testRule(initiativeId, RewardGroupsCondition, transaction, true);

        transaction.setEffectiveAmountCents(upperBound);
        testRule(initiativeId, RewardGroupsCondition, transaction, true);

        transaction.setEffectiveAmountCents(1038L);
        testRule(initiativeId, RewardGroupsCondition, transaction, false);
    }

}
