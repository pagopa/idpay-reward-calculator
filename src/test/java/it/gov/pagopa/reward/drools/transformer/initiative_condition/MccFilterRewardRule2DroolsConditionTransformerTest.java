package it.gov.pagopa.reward.drools.transformer.initiative_condition;

import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;
import it.gov.pagopa.reward.model.RewardTransaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

public class MccFilterRewardRule2DroolsConditionTransformerTest extends InitiativeRewardRule2DroolsConditionTransformerTest {

    private final MccFilterRewardRule2DroolsConditionTransformer transformer = new MccFilterRewardRule2DroolsConditionTransformer();

    @Test
    public void testMccFilter(){
        // testing allowed list
        MccFilterDTO initiativeRewardRule = new MccFilterDTO();
        initiativeRewardRule.setAllowedList(true);
        initiativeRewardRule.setValues(new TreeSet<>(Set.of("MCC", "MCC2")));
        String mccCondition = transformer.apply(initiativeRewardRule);

        Assertions.assertEquals("mcc in (\"MCC\",\"MCC2\")", mccCondition);

        RewardTransaction transaction = new RewardTransaction();
        transaction.setMcc("MCC");
        testRule("MccFilter", mccCondition, transaction, true);

        // testing not allowed list
        initiativeRewardRule.setAllowedList(false);
        String mccNotCondition = transformer.apply(initiativeRewardRule);

        Assertions.assertEquals("mcc not in (\"MCC\",\"MCC2\")", mccNotCondition);
        testRule("MccFilter", mccNotCondition, transaction, false);
    }

}
