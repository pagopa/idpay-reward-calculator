package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;

import java.math.BigDecimal;

class RewardValueTrxConsequence2DroolsRuleTransformerTest extends InitiativeTrxConsequence2DroolsRuleTransformerTest<RewardValueDTO> {

    private final RewardValueTrxConsequence2DroolsRuleTransformer transformer = new RewardValueTrxConsequence2DroolsRuleTransformer(new TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl());
    private final RewardValueDTO rewardValueDTO = RewardValueDTO.builder().rewardValue(BigDecimal.valueOf(12.25)).build();

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
        return """
                
                rule "ruleName-REWARDVALUE"
                salience -1
                agenda-group "initiativeId"
                when
                   $userCounters: it.gov.pagopa.reward.model.counters.UserInitiativeCounters()
                   $initiativeCounters: it.gov.pagopa.reward.model.counters.InitiativeCounters() from $userCounters.initiatives.getOrDefault("initiativeId", new it.gov.pagopa.reward.model.counters.InitiativeCounters("initiativeId"))
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval($trx.getInitiativeRejectionReasons().get("initiativeId") == null)
                then $trx.getRewards().put("initiativeId", new it.gov.pagopa.reward.dto.trx.Reward("initiativeId","organizationId",$trx.getEffectiveAmount().multiply(new java.math.BigDecimal("0.1225")).setScale(2, java.math.RoundingMode.HALF_DOWN)));
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
        return bigDecimalValue(1.38);
    }
}
