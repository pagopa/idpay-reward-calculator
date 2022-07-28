package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.model.RewardTransaction;

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
                when $trx: it.gov.pagopa.reward.model.RewardTransaction(rejectionReason.size() == 0)
                then $trx.getRewards().put("agendaGroup", $trx.getAmount().multiply(new java.math.BigDecimal("0.1225")).setScale(2, java.math.RoundingMode.HALF_DOWN));
                end
                """;
    }

    @Override
    protected RewardTransaction getTransaction() {
        RewardTransaction trx =new RewardTransaction();
        trx.setAmount(BigDecimal.valueOf(11.25));
        return trx;
    }

    @Override
    protected BigDecimal getExpectedReward() {
        return BigDecimal.valueOf(1.38).setScale(2, RoundingMode.UNNECESSARY);
    }
}
