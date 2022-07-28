package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.reward.drools.model.DroolsRuleTemplateParam;
import it.gov.pagopa.reward.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;

import java.util.stream.Collectors;

public class RewardGroupsTrxConsequence2DroolsExpressionTransformer implements InitiativeTrxConsequence2DroolsExpressionTransformer<RewardGroupsDTO> {
    @Override
    public String apply(RewardGroupsDTO trxConsequence) {
        return RewardValueTrxConsequence2DroolsExpressionTransformer.applyPercentReward(
                new DroolsRuleTemplateParam(
                        "%s:java.math.BigDecimal.ZERO".formatted(
                                trxConsequence.getRewardGroups().stream()
                                        .map(rg -> "($trx.getAmount().compareTo(%s)>=0 && $trx.getAmount().compareTo(%s)<=0)?%s".formatted(
                                                        DroolsTemplateRuleUtils.toTemplateParam(rg.getFrom()),
                                                        DroolsTemplateRuleUtils.toTemplateParam(rg.getTo()),
                                                        RewardValueTrxConsequence2DroolsExpressionTransformer.rewardPercentValue2TemplateParam(rg.getRewardValue())
                                                )
                                        )
                                        .collect(Collectors.joining(":"))
                        )
                ));
    }
}
