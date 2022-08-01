package it.gov.pagopa.reward.drools.transformer.conditions.predicates;

import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.model.counters.InitiativeCounters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.api.command.Command;
import org.kie.internal.command.CommandFactory;

import java.util.ArrayList;
import java.util.List;

class TrxCountTrxCondition2DroolsConditionTransformerTest extends InitiativeTrxCondition2DroolsConditionTransformerTest {

    private final TrxCountTrxCondition2DroolsConditionTransformer transformer = new TrxCountTrxCondition2DroolsConditionTransformer();

    private final long lowerBound = 1L;
    private final long upperBound = 10L;

    private long trxNumber;

    @Override
    protected List<Command<?>> buildKieContainerCommands(TransactionDroolsDTO trx, String agendaGroup) {
        final InitiativeCounters counter = new InitiativeCounters();
        counter.setTrxNumber(trxNumber);

        final List<Command<?>> cmds = new ArrayList<>(super.buildKieContainerCommands(trx, agendaGroup));
        cmds.add(CommandFactory.newInsert(counter));
        return cmds;
    }

    @Override
    protected String buildCondition(String rewardCondition) {
        return """
                $initiativeCounters: %s()
                %s""".formatted(
                InitiativeCounters.class.getName(),
                super.buildCondition(rewardCondition)
        );
    }

    private void testLowerBound(String thresholdCondition, TransactionDroolsDTO transaction, boolean expectedBefore, boolean expectedEqual) {
        trxNumber = lowerBound - 2;
        testRule("TrxCount", thresholdCondition, transaction, expectedBefore);

        trxNumber = lowerBound - 1;
        testRule("TrxCount", thresholdCondition, transaction, expectedEqual);

        trxNumber = lowerBound;
        testRule("TrxCount", thresholdCondition, transaction, true);
    }

    private void testUpperBound(String thresholdCondition, TransactionDroolsDTO transaction, boolean expectedEqual, boolean expectedGreater) {
        trxNumber = upperBound - 2;
        testRule("TrxCount", thresholdCondition, transaction, true);

        trxNumber = upperBound - 1;
        testRule("TrxCount", thresholdCondition, transaction, expectedEqual);

        trxNumber = upperBound;
        testRule("TrxCount", thresholdCondition, transaction, expectedGreater);
    }

    @Test
    void testTrxCountMin() {
        TrxCountDTO initiativeTrxCondition = new TrxCountDTO();
        initiativeTrxCondition.setFrom(lowerBound);
        initiativeTrxCondition.setFromIncluded(false);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("$initiativeCounters.trxNumber > new java.lang.Long(\"0\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, false);
        testUpperBound(thresholdCondition, transaction, true, true);
    }

    @Test
    void testTrxCountMinEqual() {
        TrxCountDTO initiativeTrxCondition = new TrxCountDTO();
        initiativeTrxCondition.setFrom(lowerBound);
        initiativeTrxCondition.setFromIncluded(true);
        String trxCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("$initiativeCounters.trxNumber >= new java.lang.Long(\"0\")", trxCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(trxCondition, transaction, false, true);
        testUpperBound(trxCondition, transaction, true, true);
    }

    @Test
    void testTrxCountMax() {
        TrxCountDTO initiativeTrxCondition = new TrxCountDTO();
        initiativeTrxCondition.setTo(upperBound);
        initiativeTrxCondition.setToIncluded(false);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("$initiativeCounters.trxNumber < new java.lang.Long(\"9\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, true, true);
        testUpperBound(thresholdCondition, transaction, false, false);
    }

    @Test
    void testTrxCountMaxEqual() {
        TrxCountDTO initiativeTrxCondition = new TrxCountDTO();
        initiativeTrxCondition.setTo(upperBound);
        initiativeTrxCondition.setToIncluded(true);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("$initiativeCounters.trxNumber <= new java.lang.Long(\"9\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, true, true);
        testUpperBound(thresholdCondition, transaction, true, false);
    }

    @Test
    void testTrxCountBetweenClosed() {
        TrxCountDTO initiativeTrxCondition = new TrxCountDTO();
        initiativeTrxCondition.setFrom(lowerBound);
        initiativeTrxCondition.setFromIncluded(true);
        initiativeTrxCondition.setTo(upperBound);
        initiativeTrxCondition.setToIncluded(true);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("$initiativeCounters.trxNumber >= new java.lang.Long(\"0\") && $initiativeCounters.trxNumber <= new java.lang.Long(\"9\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, true);
        testUpperBound(thresholdCondition, transaction, true, false);
    }

    @Test
    void testTrxCountBetweenOpen() {
        TrxCountDTO initiativeTrxCondition = new TrxCountDTO();
        initiativeTrxCondition.setFrom(lowerBound);
        initiativeTrxCondition.setFromIncluded(false);
        initiativeTrxCondition.setTo(upperBound);
        initiativeTrxCondition.setToIncluded(false);
        String thresholdCondition = transformer.apply(initiativeTrxCondition);

        Assertions.assertEquals("$initiativeCounters.trxNumber > new java.lang.Long(\"0\") && $initiativeCounters.trxNumber < new java.lang.Long(\"9\")", thresholdCondition);

        TransactionDroolsDTO transaction = new TransactionDroolsDTO();

        testLowerBound(thresholdCondition, transaction, false, false);
        testUpperBound(thresholdCondition, transaction, false, false);
    }

}
