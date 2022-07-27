package it.gov.pagopa.reward.drools.transformer.consequences;

import it.gov.pagopa.reward.drools.transformer.consequences.expressions.InitiativeTrxConsequence2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardGroupsTrxConsequence2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardLimitsTrxConsequence2DroolsExpressionTransformer;
import it.gov.pagopa.reward.drools.transformer.consequences.expressions.RewardValueTrxConsequence2DroolsExpressionTransformer;
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

public class TrxConsequence2DroolsRewardExpressionTransformerFacadeTest {

    private static final RewardValueTrxConsequence2DroolsExpressionTransformer rewardValueTransformerMock = Mockito.mock(RewardValueTrxConsequence2DroolsExpressionTransformer.class);
    private static final RewardGroupsTrxConsequence2DroolsExpressionTransformer rewardGroupsTransformerMock = Mockito.mock(RewardGroupsTrxConsequence2DroolsExpressionTransformer.class);
    private static final RewardLimitsTrxConsequence2DroolsExpressionTransformer rewardLimitsTransformerMock = Mockito.mock(RewardLimitsTrxConsequence2DroolsExpressionTransformer.class);

    private static final List<InitiativeTrxConsequence2DroolsExpressionTransformer<?>> mocks =List.of(
            rewardValueTransformerMock,
            rewardGroupsTransformerMock,
            rewardLimitsTransformerMock
    );

    private static final TrxConsequence2DroolsRewardExpressionTransformerFacade transformer = new TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl();


    @BeforeAll
    public static void removeFinalModifiers() throws IllegalAccessException {
        configureMock(rewardValueTransformerMock, "rewardValueTrxConsequenceTransformer");
        configureMock(rewardGroupsTransformerMock, "rewardGroupsTrxConsequenceTransformer");
        configureMock(rewardLimitsTransformerMock, "rewardLimitsTrxConsequenceTransformer");
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(rewardValueTransformerMock, rewardGroupsTransformerMock, rewardLimitsTransformerMock);
    }

    private static void configureMock(InitiativeTrxConsequence2DroolsExpressionTransformer<?> mock, String fieldName) throws IllegalAccessException {
        Field field = ReflectionUtils.findField(TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl.class, fieldName);
        Assertions.assertNotNull(field);
        field.setAccessible(true);
        field.set(transformer, mock);
    }

    private <T extends InitiativeTrxConsequence> void test(T trxConsequence, InitiativeTrxConsequence2DroolsExpressionTransformer<T> mock){
        transformer.apply(trxConsequence);
        Mockito.verify(mock).apply(Mockito.same(trxConsequence));

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
            transformer.apply(new InitiativeTrxConsequence() {});
            Assertions.fail("Exception expected");
        } catch (IllegalStateException e){
            // Do nothing
        } catch (Exception e){
            Assertions.fail("Unexpected exception", e);
        }
    }

}
