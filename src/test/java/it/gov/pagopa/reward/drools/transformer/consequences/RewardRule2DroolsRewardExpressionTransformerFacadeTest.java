package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.drools.transformer.consequences.expressions.InitiativeRewardRule2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardGroupsRewardRule2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardLimitsRewardRule2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardValueRewardRule2DroolsExpressionTransformer;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

public class RewardRule2DroolsRewardExpressionTransformerFacadeTest {

    private static final RewardValueRewardRule2DroolsExpressionTransformer rewardValueTransformerMock = Mockito.mock(RewardValueRewardRule2DroolsExpressionTransformer.class);
    private static final RewardGroupsRewardRule2DroolsExpressionTransformer rewardGroupsTransformerMock = Mockito.mock(RewardGroupsRewardRule2DroolsExpressionTransformer.class);
    private static final RewardLimitsRewardRule2DroolsExpressionTransformer rewardLimitsTransformerMock = Mockito.mock(RewardLimitsRewardRule2DroolsExpressionTransformer.class);

    private static final List<InitiativeRewardRule2DroolsExpressionTransformer<?>> mocks =List.of(
            rewardValueTransformerMock,
            rewardGroupsTransformerMock,
            rewardLimitsTransformerMock
    );

    private static final RewardRule2DroolsRewardExpressionTransformerFacade transformer = new RewardRule2DroolsRewardExpressionTransformerFacadeImpl();


    @BeforeAll
    public static void removeFinalModifiers() throws IllegalAccessException {
        configureMock(rewardValueTransformerMock, "rewardValueRewardRuleTransformer");
        configureMock(rewardGroupsTransformerMock, "rewardGroupsRewardRuleTransformer");
        configureMock(rewardLimitsTransformerMock, "rewardLimitsRewardRuleTransformer");
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(rewardValueTransformerMock, rewardGroupsTransformerMock, rewardLimitsTransformerMock);
    }

    private static void configureMock(InitiativeRewardRule2DroolsExpressionTransformer<?> mock, String fieldName) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(RewardRule2DroolsRewardExpressionTransformerFacadeImpl.class, fieldName);
        Assertions.assertNotNull(field);
        field.setAccessible(true);
        field.set(transformer, mock);
    }

    private <T extends InitiativeRewardRule> void test(T rewardRule, InitiativeRewardRule2DroolsExpressionTransformer<T> mock){
        transformer.apply(rewardRule);
        Mockito.verify(mock).apply(Mockito.same(rewardRule));

        Mockito.verifyNoMoreInteractions(mock);
        mocks.stream()
                .filter(m->m!=mock)
                .forEach(Mockito::verifyNoInteractions);
    }

    @Test
    public void testRewardValue() {
        test(new RewardValueDTO(), rewardValueTransformerMock);
    }

    @Test
    public void testRewardGroups() {
        test(new RewardGroupsDTO(), rewardGroupsTransformerMock);
    }

    @Test
    public void testRewardLimits() {
        test(new RewardLimitsDTO(), rewardLimitsTransformerMock);
    }

    @Test
    public void testNotHandled() {
        try{
            transformer.apply(new InitiativeRewardRule() {});
            Assertions.fail("Exception expected");
        } catch (IllegalStateException e){
            // Do nothing
        } catch (Exception e){
            Assertions.fail("Unexpected exception", e);
        }
    }

}
