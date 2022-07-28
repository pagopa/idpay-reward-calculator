package it.gov.pagopa.reward.drools.transformer.conditions;

import it.gov.pagopa.reward.drools.transformer.conditions.rules.*;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.trx.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

class TrxCondition2DroolsRuleTransformerFacadeTest {

    private static final TrxCondition2DroolsConditionTransformerFacade conditionTransformerFacadeMock = Mockito.mock(TrxCondition2DroolsConditionTransformerFacade.class);

    private static final DayOfWeekTrxCondition2DroolsRuleTransformer dayOfWeekTrxConditionTransformerMock = Mockito.mock(DayOfWeekTrxCondition2DroolsRuleTransformer.class);
    private static final MccFilterTrxCondition2DroolsRuleTransformer mccTrxConditionTransformerMock = Mockito.mock(MccFilterTrxCondition2DroolsRuleTransformer.class);
    private static final RewardLimitsTrxCondition2DroolsRuleTransformer rewardLimitsTrxConditionTransformerMock = Mockito.mock(RewardLimitsTrxCondition2DroolsRuleTransformer.class);
    private static final ThresholdTrxCondition2DroolsRuleTransformer thresholdTrxConditionTransformerMock = Mockito.mock(ThresholdTrxCondition2DroolsRuleTransformer.class);
    private static final TrxCountTrxCondition2DroolsRuleTransformer trxCountTrxConditionTransformerMock = Mockito.mock(TrxCountTrxCondition2DroolsRuleTransformer.class);
    private static final RewardGroupsTrxCondition2DroolsRuleTransformer rewardGroupsTrxConditionTransformerMock = Mockito.mock(RewardGroupsTrxCondition2DroolsRuleTransformer.class);

    private static final List<InitiativeTrxCondition2DroolsRuleTransformer<?>> mocks = List.of(
            dayOfWeekTrxConditionTransformerMock,
            mccTrxConditionTransformerMock,
            rewardLimitsTrxConditionTransformerMock,
            thresholdTrxConditionTransformerMock,
            trxCountTrxConditionTransformerMock,
            rewardGroupsTrxConditionTransformerMock
    );

    private static final TrxCondition2DroolsRuleTransformerFacadeImpl transformer = new TrxCondition2DroolsRuleTransformerFacadeImpl(conditionTransformerFacadeMock);


    @BeforeAll
    public static void removeFinalModifiers() throws IllegalAccessException {
        configureMock(dayOfWeekTrxConditionTransformerMock, "dayOfWeekTrxConditionTransformer");
        configureMock(mccTrxConditionTransformerMock, "mccFilterTrxConditionTransformer");
        configureMock(rewardLimitsTrxConditionTransformerMock, "rewardLimitsTrxConditionTransformer");
        configureMock(thresholdTrxConditionTransformerMock, "thresholdTrxConditionTransformer");
        configureMock(trxCountTrxConditionTransformerMock, "trxCountTrxConditionTransformer");
        configureMock(rewardGroupsTrxConditionTransformerMock, "rewardGroupsTrxConditionTransformer");
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(conditionTransformerFacadeMock, dayOfWeekTrxConditionTransformerMock, mccTrxConditionTransformerMock, rewardLimitsTrxConditionTransformerMock, thresholdTrxConditionTransformerMock, trxCountTrxConditionTransformerMock, rewardGroupsTrxConditionTransformerMock);
    }

    private static void configureMock(InitiativeTrxCondition2DroolsRuleTransformer<?> mock, String fieldName) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(TrxCondition2DroolsRuleTransformerFacadeImpl.class, fieldName);
        Assertions.assertNotNull(field);
        field.setAccessible(true);
        field.set(transformer, mock);
    }

    private <T extends InitiativeTrxCondition> void test(T initiativeTrxCondition, InitiativeTrxCondition2DroolsRuleTransformer<T> mock) {
        transformer.apply("AGENDAGROUP", "RULENAME", initiativeTrxCondition);
        Mockito.verify(mock).apply(Mockito.eq("AGENDAGROUP"), Mockito.eq("RULENAME"), Mockito.same(initiativeTrxCondition));

        Mockito.verifyNoMoreInteractions(mock);
        mocks.stream()
                .filter(m -> m != mock)
                .forEach(Mockito::verifyNoInteractions);
    }

    @Test
    void testDayOfWeek() {
        test(new DayOfWeekDTO(), dayOfWeekTrxConditionTransformerMock);
    }

    @Test
    void testMccFilter() {
        test(new MccFilterDTO(), mccTrxConditionTransformerMock);
    }

    @Test
    void testRewardLimits() {
        test(new RewardLimitsDTO(), rewardLimitsTrxConditionTransformerMock);
    }

    @Test
    void testThreshold() {
        test(new ThresholdDTO(), thresholdTrxConditionTransformerMock);
    }

    @Test
    void testTrxCount() {
        test(new TrxCountDTO(), trxCountTrxConditionTransformerMock);
    }

    @Test
    void testRewardGroups() {
        test(new RewardGroupsDTO(), rewardGroupsTrxConditionTransformerMock);
    }

    @Test
    void testNotHandled() {
        InitiativeTrxCondition notHandledTrxCondition = new InitiativeTrxCondition() {};
        try {
            transformer.apply("AGENDAGROUP", "RULENAME", notHandledTrxCondition);
            Assertions.fail("Exception expected");
        } catch (IllegalStateException e) {
            // Do nothing
        }
    }

    @Test
    void testNull() {
        String result = transformer.apply("AGENDAGROUP", "RULENAME", null);
        Assertions.assertEquals("", result);
    }

}
