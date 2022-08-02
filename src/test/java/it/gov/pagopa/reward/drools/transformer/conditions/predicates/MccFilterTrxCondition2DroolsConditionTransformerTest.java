package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.trx.MccFilterDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

class MccFilterTrxCondition2DroolsConditionTransformerTest extends InitiativeTrxCondition2DroolsConditionTransformerTest {

    private final String initiativeId = "MccFilter";
    private final MccFilterTrxCondition2DroolsConditionTransformer transformer = new MccFilterTrxCondition2DroolsConditionTransformer();

    @Test
    void testMccFilter(){
        // testing allowed list
        MccFilterDTO initiativeTrxCondition = new MccFilterDTO();
        initiativeTrxCondition.setAllowedList(true);
        initiativeTrxCondition.setValues(new TreeSet<>(Set.of("MCC", "MCC2")));
        String mccCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("mcc in (\"MCC\",\"MCC2\")", mccCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();
        transaction.setMcc("MCC");
        testRule(initiativeId, mccCondition, transaction, true);

        // testing not allowed list
        initiativeTrxCondition.setAllowedList(false);
        String mccNotCondition = transformer.apply(initiativeId, initiativeTrxCondition);

        Assertions.assertEquals("mcc not in (\"MCC\",\"MCC2\")", mccNotCondition);
        testRule(initiativeId, mccNotCondition, transaction, false);
    }

}
