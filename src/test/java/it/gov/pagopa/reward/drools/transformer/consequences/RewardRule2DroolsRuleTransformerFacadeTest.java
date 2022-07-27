package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.drools.transformer.consequences.rules.InitiativeRewardRule2DroolsRuleTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardGroupsRewardRule2DroolsRuleTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardLimitsRewardRule2DroolsRuleTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardValueRewardRule2DroolsRuleTransformer;
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

public class RewardRule2DroolsRuleTransformerFacadeTest {

    private static final RewardRule2DroolsRewardExpressionTransformerFacade expressionTransformerFacadeMock = Mockito.mock(RewardRule2DroolsRewardExpressionTransformerFacade.class);

    private static final RewardValueRewardRule2DroolsRuleTransformer rewardValueTransformerMock = Mockito.mock(RewardValueRewardRule2DroolsRuleTransformer.class);
    private static final RewardGroupsRewardRule2DroolsRuleTransformer rewardGroupsTransformerMock = Mockito.mock(RewardGroupsRewardRule2DroolsRuleTransformer.class);
    private static final RewardLimitsRewardRule2DroolsRuleTransformer rewardLimitsTransformerMock = Mockito.mock(RewardLimitsRewardRule2DroolsRuleTransformer.class);

    private static final List<InitiativeRewardRule2DroolsRuleTransformer<?>> mocks = List.of(
            rewardValueTransformerMock,
            rewardGroupsTransformerMock,
            rewardLimitsTransformerMock
    );

    private static final RewardRule2DroolsRuleTransformerFacadeImpl transformer = new RewardRule2DroolsRuleTransformerFacadeImpl(expressionTransformerFacadeMock);

    @BeforeAll
    public static void removeFinalModifiers() throws IllegalAccessException {
        configureMock(rewardValueTransformerMock, "rewardValueRewardRuleTransformer");
        configureMock(rewardGroupsTransformerMock, "rewardGroupsRewardRuleTransformer");
        configureMock(rewardLimitsTransformerMock, "rewardLimitsRewardRuleTransformer");
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(expressionTransformerFacadeMock, rewardValueTransformerMock, rewardGroupsTransformerMock, rewardLimitsTransformerMock);
    }

    private static void configureMock(InitiativeRewardRule2DroolsRuleTransformer<?> mock, String fieldName) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(RewardRule2DroolsRuleTransformerFacadeImpl.class, fieldName);
        Assertions.assertNotNull(field);
        field.setAccessible(true);
        field.set(transformer, mock);
    }

    private <T extends InitiativeRewardRule> void test(T rewardRule, InitiativeRewardRule2DroolsRuleTransformer<T> mock) {
        transformer.apply("AGENDAGROUP", "RULENAME", rewardRule);
        Mockito.verify(mock).apply(Mockito.eq("AGENDAGROUP"), Mockito.eq("RULENAME"), Mockito.same(rewardRule));

        Mockito.verifyNoMoreInteractions(mock);
        mocks.stream()
                .filter(m -> m != mock)
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
        try {
            transformer.apply("AGENDAGROUP", "RULENAME", new InitiativeRewardRule() {
            });
            Assertions.fail("Exception expected");
        } catch (IllegalStateException e) {
            // Do nothing
        } catch (Exception e) {
            Assertions.fail("Unexpected exception", e);
        }
    }

    @Test
    public void testNull() {
        String result = transformer.apply("AGENDAGROUP", "RULENAME", null);
        Assertions.assertEquals("", result);
    }

}
