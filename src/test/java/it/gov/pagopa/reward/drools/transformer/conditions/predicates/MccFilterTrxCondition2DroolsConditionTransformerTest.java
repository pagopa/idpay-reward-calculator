package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;
import it.gov.pagopa.reward.model.RewardTransaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

public class MccFilterTrxCondition2DroolsConditionTransformerTest extends InitiativeTrxCondition2DroolsConditionTransformerTest {

    private final MccFilterTrxCondition2DroolsConditionTransformer transformer = new MccFilterTrxCondition2DroolsConditionTransformer();

    @Test
    public void testMccFilter(){
        // testing allowed list
        MccFilterDTO initiativeTrxCondition = new MccFilterDTO();
        initiativeTrxCondition.setAllowedList(true);
        initiativeTrxCondition.setValues(new TreeSet<>(Set.of("MCC", "MCC2")));
        String mccCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("mcc in (\"MCC\",\"MCC2\")", mccCondition);

        RewardTransaction transaction = new RewardTransaction();
        transaction.setMcc("MCC");
        testRule("MccFilter", mccCondition, transaction, true);

        // testing not allowed list
        initiativeTrxCondition.setAllowedList(false);
        String mccNotCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("mcc not in (\"MCC\",\"MCC2\")", mccNotCondition);
        testRule("MccFilter", mccNotCondition, transaction, false);
    }

}
