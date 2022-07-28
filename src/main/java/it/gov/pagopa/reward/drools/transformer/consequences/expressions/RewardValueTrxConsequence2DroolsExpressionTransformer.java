package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.reward.drools.model.DroolsRuleTemplateParam;
import it.gov.pagopa.reward.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RewardValueTrxConsequence2DroolsExpressionTransformer implements InitiativeTrxConsequence2DroolsExpressionTransformer<RewardValueDTO> {

    public static final BigDecimal ONEHUNDRED = BigDecimal.valueOf(100).setScale(4, RoundingMode.UNNECESSARY);

    @Override
    public String apply(RewardValueDTO trxConsequence) {
        return applyPercentReward(rewardPercentValue2TemplateParam(trxConsequence.getRewardValue()));
    }

    public static DroolsRuleTemplateParam rewardPercentValue2TemplateParam(BigDecimal rewardPercentValue) {
        return DroolsTemplateRuleUtils.toTemplateParam(rewardPercentValue.setScale(4, RoundingMode.UNNECESSARY).divide(ONEHUNDRED, RoundingMode.HALF_DOWN));
    }

    public static String applyPercentReward(DroolsRuleTemplateParam rewardPercentValueTemplateParam) {
        return "$trx.getAmount().multiply(%s).setScale(2, java.math.RoundingMode.HALF_DOWN)".formatted(rewardPercentValueTemplateParam.getParam());
    }
}
