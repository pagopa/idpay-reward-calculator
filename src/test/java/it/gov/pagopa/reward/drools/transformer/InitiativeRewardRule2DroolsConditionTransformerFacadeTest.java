package it.gov.pagopa.reward.drools.transformer;

import it.gov.pagopa.reward.drools.transformer.initiative_condition.*;
import it.gov.pagopa.reward.dto.rule.trx.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

public class InitiativeRewardRule2DroolsConditionTransformerFacadeTest {

    private static final DayOfWeekRewardRule2DroolsConditionTransformer dayOfWeekRewardRuleTransformerMock = Mockito.mock(DayOfWeekRewardRule2DroolsConditionTransformer.class);
    private static final MccFilterRewardRule2DroolsConditionTransformer mccTransformerMock = Mockito.mock(MccFilterRewardRule2DroolsConditionTransformer.class);
    private static final RewardLimitsRewardRule2DroolsConditionTransformer rewardLimitsRewardRuleTransformerMock = Mockito.mock(RewardLimitsRewardRule2DroolsConditionTransformer.class);
    private static final ThresholdRewardRule2DroolsConditionTransformer thresholdRewardRuleTransformerMock = Mockito.mock(ThresholdRewardRule2DroolsConditionTransformer.class);
    private static final TrxCountRewardRule2DroolsConditionTransformer trxCountRewardRuleTransformerMock = Mockito.mock(TrxCountRewardRule2DroolsConditionTransformer.class);

    private static final List<InitiativeRewardRule2DroolsConditionTransformer<?>> mocks =List.of(
            dayOfWeekRewardRuleTransformerMock,
            mccTransformerMock,
            rewardLimitsRewardRuleTransformerMock,
            thresholdRewardRuleTransformerMock,
            trxCountRewardRuleTransformerMock
    );

    private static final InitiativeRewardRule2DroolsConditionTransformerFacadeImpl transformer = new InitiativeRewardRule2DroolsConditionTransformerFacadeImpl();


    @BeforeAll
    public static void removeFinalModifiers() throws IllegalAccessException {
        configureMock(dayOfWeekRewardRuleTransformerMock, "dayOfWeekRewardRuleTransformer");
        configureMock(mccTransformerMock, "mccFilterRewardRuleTransformer");
        configureMock(rewardLimitsRewardRuleTransformerMock, "rewardLimitsRewardRuleTransformer");
        configureMock(thresholdRewardRuleTransformerMock, "thresholdRewardRuleTransformer");
        configureMock(trxCountRewardRuleTransformerMock, "trxCountRewardRuleTransformer");
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(dayOfWeekRewardRuleTransformerMock, mccTransformerMock, rewardLimitsRewardRuleTransformerMock, thresholdRewardRuleTransformerMock, trxCountRewardRuleTransformerMock);
    }

    private static void configureMock(InitiativeRewardRule2DroolsConditionTransformer<?> mock, String fieldName) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(InitiativeRewardRule2DroolsConditionTransformerFacadeImpl.class, fieldName);
        Assertions.assertNotNull(field);
        field.setAccessible(true);
        field.set(transformer, mock);
    }

    private <T extends InitiativeTrxCondition> void test(T initiativeRewardRule, InitiativeRewardRule2DroolsConditionTransformer<T> mock){
        transformer.apply(initiativeRewardRule);
        Mockito.verify(mock).apply(Mockito.same(initiativeRewardRule));

        Mockito.verifyNoMoreInteractions(mock);
        mocks.stream()
                .filter(m->m!=mock)
                .forEach(Mockito::verifyNoInteractions);
    }

    @Test
    public void testDayOfWeek() {
        test(new DayOfWeekDTO(), dayOfWeekRewardRuleTransformerMock);
    }

    @Test
    public void testMccFilter() {
        test(new MccFilterDTO(), mccTransformerMock);
    }

    @Test
    public void testRewardLimits() {
        test(new RewardLimitsDTO(), rewardLimitsRewardRuleTransformerMock);
    }

    @Test
    public void testThreshold() {
        test(new ThresholdDTO(), thresholdRewardRuleTransformerMock);
    }

    @Test
    public void testTrxCount() {
        test(new TrxCountDTO(), trxCountRewardRuleTransformerMock);
    }

    @Test
    public void testNotHandled() {
        try{
            transformer.apply(new InitiativeTrxCondition() {});
            Assertions.fail("Exception expected");
        } catch (IllegalStateException e){
            // Do nothing
        } catch (Exception e){
            Assertions.fail("Unexpected exception", e);
        }
    }

}
