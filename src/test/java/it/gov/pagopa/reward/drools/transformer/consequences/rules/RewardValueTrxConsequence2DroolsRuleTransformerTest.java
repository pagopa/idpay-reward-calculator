package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.enums.RewardValueType;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class RewardValueTrxConsequence2DroolsRuleTransformerTest extends InitiativeTrxConsequence2DroolsRuleTransformerTest<RewardValueDTO> {

    public static final String BASE_EXPECTED_RULE = """
                            
            rule "ruleName-REWARDVALUE"
            salience -1
            agenda-group "initiativeId"
            when
               $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
               $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("initiativeId", new it.gov.pagopa.reward.model.counters.InitiativeCounters("initiativeId"))
               $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
               eval($trx.getInitiativeRejectionReasons().get("initiativeId") == null)
            then $trx.getRewards().put("initiativeId", new it.gov.pagopa.reward.dto.trx.Reward("initiativeId","organizationId",%REWARD%));
            end
            """;

    private final RewardValueTrxConsequence2DroolsRuleTransformer transformer = new RewardValueTrxConsequence2DroolsRuleTransformer(new TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl());

    private RewardValueDTO rewardValueDTO;
    private String expectedRule;
    private BigDecimal expectedReward;

    @Override
    protected InitiativeTrxConsequence2DroolsRuleTransformer<RewardValueDTO> getTransformer() {
        return transformer;
    }

    @Override
    protected RewardValueDTO getInitiativeTrxConsequence() {
        return rewardValueDTO;
    }

    @Override
    protected String getExpectedRule() {
        return expectedRule;
    }

    @Override
    protected TransactionDroolsDTO getTransaction() {
        TransactionDroolsDTO trx =new TransactionDroolsDTO();
        trx.setEffectiveAmount(BigDecimal.valueOf(11.25));
        return trx;
    }

    @Override
    protected BigDecimal getExpectedReward() {
        return expectedReward;
    }


    @Test
    @Override
    void testDiscardedIfRejected() {
        configurePercentageUseCase();

        super.testDiscardedIfRejected();
    }

    // test suppressed in order to execute it using a different name
    @Override
    void testReward() {
        super.testReward();
    }

    @Test
    void testPercentageReward() {
        configurePercentageUseCase();

        testReward();
    }

    private void configurePercentageUseCase() {
        rewardValueDTO = RewardValueDTO.builder().rewardValue(BigDecimal.valueOf(12.25)).build();
        expectedRule = BASE_EXPECTED_RULE.replace("%REWARD%",
                "$trx.getEffectiveAmount().multiply(new java.math.BigDecimal(\"0.1225\")).setScale(2, java.math.RoundingMode.HALF_DOWN)");
        expectedReward = bigDecimalValue(1.38);
    }

    @Test
    void testAbsoluteReward() {
        configureAbsoluteUseCase();

        testReward();
    }

    private void configureAbsoluteUseCase() {
        rewardValueDTO = RewardValueDTO.builder().rewardValue(BigDecimal.valueOf(12.25)).rewardValueType(RewardValueType.ABSOLUTE).build();
        expectedRule = BASE_EXPECTED_RULE.replace("%REWARD%",
                "new java.math.BigDecimal(\"12.25\").setScale(2, java.math.RoundingMode.HALF_DOWN)");
        expectedReward = bigDecimalValue(12.25);
    }
}
