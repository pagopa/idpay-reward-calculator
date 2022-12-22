package it.gov.pagopa.reward.drools.transformer.consequences.rules;

import it.gov.pagopa.reward.drools.transformer.consequences.TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl;
import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;
import it.gov.pagopa.reward.model.TransactionDroolsDTO;
import it.gov.pagopa.reward.utils.RewardConstants;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

class TrxCountTrxConsequence2DroolsRuleTransformerTest extends InitiativeTrxConsequence2DroolsRuleTransformerTest<TrxCountDTO> {

    private final TrxCountTrxConsequence2DroolsRuleTransformer transformer = new TrxCountTrxConsequence2DroolsRuleTransformer(new TrxConsequence2DroolsRewardExpressionTransformerFacadeImpl());
    private final TrxCountDTO trxCountDTO = TrxCountDTO.builder().from(2L).build();

    @Override
    protected InitiativeTrxConsequence2DroolsRuleTransformer<TrxCountDTO> getTransformer() {
        return transformer;
    }

    @Override
    protected TrxCountDTO getInitiativeTrxConsequence() {
        return trxCountDTO;
    }

    @Override
    protected String getExpectedRule() {
        return """
                
                rule "ruleName-TRXCOUNT-REWARD"
                salience -3
                agenda-group "initiativeId"
                when
                   $trx: it.gov.pagopa.reward.model.TransactionDroolsDTO()
                   eval(java.util.List.of("TRX_RULE_TRXCOUNT_FAIL").equals($trx.getInitiativeRejectionReasons().get("initiativeId")))
                then $trx.getRewards().put("initiativeId", new it.gov.pagopa.reward.dto.trx.Reward("initiativeId","organizationId",java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.UNNECESSARY)));
                end
                """;
    }

    @Override
    protected TransactionDroolsDTO getTransaction() {
        return new TransactionDroolsDTO();
    }

    @Override
    protected TransactionDroolsDTO testRule(String rule, TransactionDroolsDTO trx, BigDecimal expectReward) {
        List<String> testedRejectedReasons = trx.getInitiativeRejectionReasons().get("initiativeId");
        if(testedRejectedReasons ==null){
            trx.setInitiativeRejectionReasons(new HashMap<>(trx.getInitiativeRejectionReasons()));
            trx.getInitiativeRejectionReasons().put("initiativeId", List.of(RewardConstants.InitiativeTrxConditionOrder.TRXCOUNT.getRejectionReason()));
        }
        return super.testRule(rule, trx, expectReward);
    }

    @Override
    protected BigDecimal getExpectedReward() {
        return bigDecimalValue(0);
    }
}
