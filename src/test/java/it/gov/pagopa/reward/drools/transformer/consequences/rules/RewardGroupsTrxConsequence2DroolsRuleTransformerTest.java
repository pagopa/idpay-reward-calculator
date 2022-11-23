package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.test.fakers.rule.RewardGroupsDTOFaker;

import java.math.BigDecimal;

class RewardGroupsTrxConsequence2DroolsRuleTransformerTest extends InitiativeTrxConsequence2DroolsRuleTransformerTest<RewardGroupsDTO> {

    private final RewardGroupsTrxConsequence2DroolsRuleTransformer transformer = new RewardGroupsTrxConsequence2DroolsRuleTransformer(new TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl());
    private final RewardGroupsDTO rewardGroupsDTO = RewardGroupsDTOFaker.mockInstance(1);

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
        return """
                
                rule "ruleName-REWARDGROUPS"
                salience -1
                agenda-group "initiativeId"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("initiativeId", new it.gov.pagopa.reward.model.counters.InitiativeCounters("initiativeId"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("initiativeId") == null)
                then $trx.getRewards().put("initiativeId", new it.gov.pagopa.reward.dto.trx.Reward("initiativeId","organizationId",$trx.getEffectiveAmount().multiply(($trx.getEffectiveAmount().compareTo(new java.math.BigDecimal("0"))>=0 && $trx.getEffectiveAmount().compareTo(new java.math.BigDecimal("5"))<=0)?new java.math.BigDecimal("0.1000"):($trx.getEffectiveAmount().compareTo(new java.math.BigDecimal("10"))>=0 && $trx.getEffectiveAmount().compareTo(new java.math.BigDecimal("15"))<=0)?new java.math.BigDecimal("0.2000"):java.math.BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_DOWN)));
                end
                """;
    }

    @Override
    protected TransactionDroolsDTO getTransaction() {
        TransactionDroolsDTO trx =new TransactionDroolsDTO();
        trx.setEffectiveAmount(BigDecimal.valueOf(11.25));
        return trx;
    }

    @Override
    protected BigDecimal getExpectedReward() {
        return bigDecimalValue(2.25);
    }
}
