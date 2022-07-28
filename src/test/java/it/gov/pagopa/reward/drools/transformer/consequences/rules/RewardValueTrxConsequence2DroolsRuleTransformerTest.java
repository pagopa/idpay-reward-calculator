package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
                agenda-group "agendaGroup"
                when $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO(initiativeRejectionReasons.get("agendaGroup") == null)
                then $trx.getRewards().put("agendaGroup", new it.gov.pagopa.reward.dto.Reward($trx.getAmount().multiply(new java.math.BigDecimal("0.1225")).setScale(2, java.math.RoundingMode.HALF_DOWN)));
                end
                """;
    }

    @Override
    protected TransactionDroolsDTO getTransaction() {
        TransactionDroolsDTO trx =new TransactionDroolsDTO();
        trx.setAmount(BigDecimal.valueOf(11.25));
        return trx;
    }

    @Override
    protected BigDecimal getExpectedReward() {
        return BigDecimal.valueOf(1.38).setScale(2, RoundingMode.UNNECESSARY);
    }
}
