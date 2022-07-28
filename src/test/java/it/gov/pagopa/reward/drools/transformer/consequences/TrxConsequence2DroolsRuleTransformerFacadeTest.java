package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.drools.transformer.consequences.rules.InitiativeTrxConsequence2DroolsRuleTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardGroupsTrxConsequence2DroolsRuleTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardLimitsTrxConsequence2DroolsRuleTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.rules.RewardValueTrxConsequence2DroolsRuleTransformer;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
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

public class TrxConsequence2DroolsRuleTransformerFacadeTest {

    private static final TrxConsequence2DroolsRewardExpressionTransformerFacade expressionTransformerFacadeMock = Mockito.mock(TrxConsequence2DroolsRewardExpressionTransformerFacade.class);

    private static final RewardValueTrxConsequence2DroolsRuleTransformer rewardValueTransformerMock = Mockito.mock(RewardValueTrxConsequence2DroolsRuleTransformer.class);
    private static final RewardGroupsTrxConsequence2DroolsRuleTransformer rewardGroupsTransformerMock = Mockito.mock(RewardGroupsTrxConsequence2DroolsRuleTransformer.class);
    private static final RewardLimitsTrxConsequence2DroolsRuleTransformer rewardLimitsTransformerMock = Mockito.mock(RewardLimitsTrxConsequence2DroolsRuleTransformer.class);

    private static final List<InitiativeTrxConsequence2DroolsRuleTransformer<?>> mocks = List.of(
            rewardValueTransformerMock,
            rewardGroupsTransformerMock,
            rewardLimitsTransformerMock
    );

    private static final TrxConsequence2DroolsRuleTransformerFacadeImpl transformer = new TrxConsequence2DroolsRuleTransformerFacadeImpl(expressionTransformerFacadeMock);

    @BeforeAll
    public static void removeFinalModifiers() throws IllegalAccessException {
        configureMock(rewardValueTransformerMock, "rewardValueTrxConsequenceTransformer");
        configureMock(rewardGroupsTransformerMock, "rewardGroupsTrxConsequenceTransformer");
        configureMock(rewardLimitsTransformerMock, "rewardLimitsTrxConsequenceTransformer");
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(expressionTransformerFacadeMock, rewardValueTransformerMock, rewardGroupsTransformerMock, rewardLimitsTransformerMock);
    }

    private static void configureMock(InitiativeTrxConsequence2DroolsRuleTransformer<?> mock, String fieldName) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(TrxConsequence2DroolsRuleTransformerFacadeImpl.class, fieldName);
        Assertions.assertNotNull(field);
        field.setAccessible(true);
        field.set(transformer, mock);
    }

    private <T extends InitiativeTrxConsequence> void test(T trxConsequence, InitiativeTrxConsequence2DroolsRuleTransformer<T> mock) {
        transformer.apply("AGENDAGROUP", "RULENAME", trxConsequence);
        Mockito.verify(mock).apply(Mockito.eq("AGENDAGROUP"), Mockito.eq("RULENAME"), Mockito.same(trxConsequence));

        Mockito.verifyNoMoreInteractions(mock);
        mocks.stream()
                .filter(m -> m != mock)
                .forEach(Mockito::verifyNoInteractions);
    }

    @Test
    void testRewardValue() {
        test(new RewardValueDTO(), rewardValueTransformerMock);
    }

    @Test
    void testRewardGroups() {
        test(new RewardGroupsDTO(), rewardGroupsTransformerMock);
    }

    @Test
    void testRewardLimits() {
        test(new RewardLimitsDTO(), rewardLimitsTransformerMock);
    }

    @Test
    void testNotHandled() {
        try {
            transformer.apply("AGENDAGROUP", "RULENAME", new InitiativeTrxConsequence() {
            });
            Assertions.fail("Exception expected");
        } catch (IllegalStateException e) {
            // Do nothing
        } catch (Exception e) {
            Assertions.fail("Unexpected exception", e);
        }
    }

    @Test
    void testNull() {
        String result = transformer.apply("AGENDAGROUP", "RULENAME", null);
        Assertions.assertEquals("", result);
    }

}
