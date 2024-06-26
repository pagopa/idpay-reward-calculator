package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.common.drools.model.DroolsRuleTemplateParam;
import it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.reward.BaseRewardValue;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.enums.RewardValueType;

import java.util.stream.Collectors;

public class RewardGroupsTrxConsequence2DroolsExpressionTransformer implements InitiativeTrxConsequence2DroolsExpressionTransformer<RewardGroupsDTO> {
    @Override
    public String apply(String initiativeId, RewardGroupsDTO trxConsequence) {
        boolean isAllPercentage = trxConsequence.getRewardGroups().stream().map(BaseRewardValue::getRewardValueType).allMatch(RewardValueType.PERCENTAGE::equals);

        String rewardValue = "%s:%s".formatted(
                trxConsequence.getRewardGroups().stream()
                        .map(rg -> "($trx.getEffectiveAmountCents().compareTo(%s)>=0 && $trx.getEffectiveAmountCents().compareTo(%s)<=0)?%s".formatted(
                                        DroolsTemplateRuleUtils.toTemplateParam(rg.getFromCents()),
                                        DroolsTemplateRuleUtils.toTemplateParam(rg.getToCents()),
                                        isAllPercentage
                                                ? RewardValueTrxConsequence2DroolsExpressionTransformer.rewardPercentValue2TemplateParam(rg.getRewardValue())
                                                : RewardValueTrxConsequence2DroolsExpressionTransformer.applyRewardValue(rg)
                                )
                        )
                        .collect(Collectors.joining(":")),
                isAllPercentage ? "java.math.BigDecimal.ZERO" : "0L"
        );

        return isAllPercentage
                ? RewardValueTrxConsequence2DroolsExpressionTransformer.applyPercentageReward(new DroolsRuleTemplateParam(rewardValue))
                : rewardValue;
    }
}
