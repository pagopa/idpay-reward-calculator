package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.drools.transformer.consequences.rules.*;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeTrxConsequence;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

class TrxConsequence2DroolsRuleTransformerFacadeTest {

    private static final TrxConsequence2DroolsRewardExpressionTransformerFacade expressionTransformerFacadeMock = Mockito.mock(TrxConsequence2DroolsRewardExpressionTransformerFacade.class);

    private static final RewardValueTrxConsequence2DroolsRuleTransformer rewardValueTransformerMock = Mockito.mock(RewardValueTrxConsequence2DroolsRuleTransformer.class);
    private static final RewardGroupsTrxConsequence2DroolsRuleTransformer rewardGroupsTransformerMock = Mockito.mock(RewardGroupsTrxConsequence2DroolsRuleTransformer.class);
    private static final RewardLimitsTrxConsequence2DroolsRuleTransformer rewardLimitsTransformerMock = Mockito.mock(RewardLimitsTrxConsequence2DroolsRuleTransformer.class);
    private static final TrxCountTrxConsequence2DroolsRuleTransformer trxCountTransformerMock = Mockito.mock(TrxCountTrxConsequence2DroolsRuleTransformer.class);

    private static final List<InitiativeTrxConsequence2DroolsRuleTransformer<?>> mocks = List.of(
            rewardValueTransformerMock,
            rewardGroupsTransformerMock,
            rewardLimitsTransformerMock,
            trxCountTransformerMock
    );

    private static final TrxConsequence2DroolsRuleTransformerFacadeImpl transformer = new TrxConsequence2DroolsRuleTransformerFacadeImpl(expressionTransformerFacadeMock);

    @BeforeAll
    public static void removeFinalModifiers() throws IllegalAccessException {
        configureMock(rewardValueTransformerMock, "rewardValueTrxConsequenceTransformer");
        configureMock(rewardGroupsTransformerMock, "rewardGroupsTrxConsequenceTransformer");
        configureMock(rewardLimitsTransformerMock, "rewardLimitsTrxConsequenceTransformer");
        configureMock(trxCountTransformerMock, "trxCountTrxConsequenceTransformer");
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(expressionTransformerFacadeMock, rewardValueTransformerMock, rewardGroupsTransformerMock, rewardLimitsTransformerMock, trxCountTransformerMock);
    }

    private static void configureMock(InitiativeTrxConsequence2DroolsRuleTransformer<?> mock, String fieldName) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(TrxConsequence2DroolsRuleTransformerFacadeImpl.class, fieldName);
        Assertions.assertNotNull(field);
        field.setAccessible(true);
        field.set(transformer, mock);
    }

    private <T extends InitiativeTrxConsequence> void test(T trxConsequence, InitiativeTrxConsequence2DroolsRuleTransformer<T> mock) {
        transformer.apply("INITIATIVEID", "ORGANIZATIONID", "RULENAME", trxConsequence);
        Mockito.verify(mock).apply(Mockito.eq("INITIATIVEID"), Mockito.eq("ORGANIZATIONID"), Mockito.eq("RULENAME"), Mockito.same(trxConsequence));

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
    void testTrxCount() {
        test(new TrxCountDTO(), trxCountTransformerMock);
    }

    @Test
    void testNotHandled() {
        InitiativeTrxConsequence notHandledTrxConsequence = new InitiativeTrxConsequence() {};
        try {
            transformer.apply("INITIATIVEID", "ORGANIZATIONID", "RULENAME", notHandledTrxConsequence);
            Assertions.fail("Exception expected");
        } catch (IllegalStateException e) {
            // Do nothing
        }
    }

    @Test
    void testNull() {
        String result = transformer.apply("INITIATIVEID", "ORGANIZATIONID", "RULENAME", null);
        Assertions.assertEquals("", result);
    }

}
