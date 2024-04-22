package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.enums.RewardValueType;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.test.fakers.rule.RewardGroupsDTOFaker;
import org.junit.jupiter.api.Test;

class RewardGroupsTrxConsequence2DroolsRuleTransformerTest extends InitiativeTrxConsequence2DroolsRuleTransformerTest<RewardGroupsDTO> {

    public final static String BASE_EXPECTED_RULE = """
                            
            rule "ruleName-REWARDGROUPS"
            salience -1
            agenda-group "initiativeId"
            when
               $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCountersWrapper()
               $userInitiativeCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters() from $userCounters.initiatives.getOrDefault("initiativeId", new it.gov.pagopa.reward.model.counters.UserInitiativeCounters("DUMMYUSERID", "initiativeId"))
               $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
               eval($trx.getInitiativeRejectionReasons().get("initiativeId") == null)
            then $trx.getRewards().put("initiativeId", new it.gov.pagopa.reward.dto.trx.Reward("initiativeId","organizationId",%REWARD%);
            end
            """;

    private final RewardGroupsTrxConsequence2DroolsRuleTransformer transformer = new RewardGroupsTrxConsequence2DroolsRuleTransformer(new TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl());
    private RewardGroupsDTO rewardGroupsDTO;
    private String expectedRule;
    private Long expectedReward;

    @Override
    protected InitiativeTrxConsequence2DroolsRuleTransformer<RewardGroupsDTO> getTransformer() {
        return transformer;
    }

    @Override
    protected RewardGroupsDTO getInitiativeTrxConsequence() {
        return rewardGroupsDTO;
    }

    @Override
    protected String getExpectedRule() {
        return expectedRule;
    }

    @Override
    protected TransactionDroolsDTO getTransaction() {
        TransactionDroolsDTO trx = new TransactionDroolsDTO();
        trx.setEffectiveAmountCents(11_25L);
        return trx;
    }

    @Override
    protected Long getExpectedReward() {
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
        rewardGroupsDTO = RewardGroupsDTOFaker.mockInstance(1);
        expectedRule = BASE_EXPECTED_RULE.replace("%REWARD%",
                "new java.math.BigDecimal($trx.getEffectiveAmountCents()).multiply(($trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"0\"))>=0 && $trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"500\"))<=0)?new java.math.BigDecimal(\"0.1000\"):($trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"1000\"))>=0 && $trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"1500\"))<=0)?new java.math.BigDecimal(\"0.2000\"):java.math.BigDecimal.ZERO).setScale(0, java.math.RoundingMode.HALF_DOWN).longValue())");
        expectedReward = 225L;
    }

    @Test
    void testAbsoluteReward() {
        configureAbsoluteUseCase();

        testReward();
    }

    private void configureAbsoluteUseCase() {
        rewardGroupsDTO = RewardGroupsDTOFaker.mockInstance(1);
        rewardGroupsDTO.getRewardGroups().forEach(r->r.setRewardValueType(RewardValueType.ABSOLUTE));

        expectedRule = BASE_EXPECTED_RULE.replace("%REWARD%",
                "($trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"0\"))>=0 && $trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"500\"))<=0)?new java.math.BigDecimal(\"10\").longValue():($trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"1000\"))>=0 && $trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"1500\"))<=0)?new java.math.BigDecimal(\"20\").longValue():0L)");
        expectedReward = 20L;
    }

    @Test
    void testMixedReward_rewardPercentage() {
        configurePercentageUseCase();

        rewardGroupsDTO.getRewardGroups().get(0).setRewardValueType(RewardValueType.ABSOLUTE);
        expectedRule = BASE_EXPECTED_RULE.replace("%REWARD%",
                "($trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"0\"))>=0 && $trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"500\"))<=0)?new java.math.BigDecimal(\"10\").longValue():($trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"1000\"))>=0 && $trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"1500\"))<=0)?new java.math.BigDecimal($trx.getEffectiveAmountCents()).multiply(new java.math.BigDecimal(\"0.2000\")).setScale(0, java.math.RoundingMode.HALF_DOWN).longValue():0L)");

        testReward();
    }

    @Test
    void testMixedReward_rewardAbsolute() {
        configureAbsoluteUseCase();

        rewardGroupsDTO.getRewardGroups().get(0).setRewardValueType(RewardValueType.PERCENTAGE);
        expectedRule = BASE_EXPECTED_RULE.replace("%REWARD%",
                "($trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"0\"))>=0 && $trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"500\"))<=0)?new java.math.BigDecimal($trx.getEffectiveAmountCents()).multiply(new java.math.BigDecimal(\"0.1000\")).setScale(0, java.math.RoundingMode.HALF_DOWN).longValue():($trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"1000\"))>=0 && $trx.getEffectiveAmountCents().compareTo(new java.lang.Long(\"1500\"))<=0)?new java.math.BigDecimal(\"20\").longValue():0L)");

        testReward();
    }

}
