package it.gov.pagopa.reward.drools.transformer.consequences.expressions;

import it.gov.pagopa.common.drools.model.DroolsRuleTemplateParam;
import it.gov.pagopa.common.drools.utils.DroolsTemplateRuleUtils;
import it.gov.pagopa.reward.dto.rule.reward.BaseRewardValue;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.enums.RewardValueType;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RewardValueTrxConsequence2DroolsExpressionTransformer implements InitiativeTrxConsequence2DroolsExpressionTransformer<RewardValueDTO> {

    public static final BigDecimal ONEHUNDRED = BigDecimal.valueOf(100).setScale(4, RoundingMode.UNNECESSARY);

    @Override
    public String apply(String initiativeId, RewardValueDTO trxConsequence) {
        return applyRewardValue(trxConsequence);
    }

    public static String applyRewardValue(BaseRewardValue trxConsequence) {
        if(RewardValueType.ABSOLUTE.equals(trxConsequence.getRewardValueType())){
            return applyAbsoluteReward(trxConsequence.getRewardValue());
        } else {
            return applyPercentageReward(rewardPercentValue2TemplateParam(trxConsequence.getRewardValue()));
        }
    }

    public static String applyAbsoluteReward(BigDecimal rewardValueAbsolute) {
        return DroolsTemplateRuleUtils.toTemplateParam(rewardValueAbsolute).getParam()+".setScale(2, java.math.RoundingMode.HALF_DOWN)";
    }

    public static DroolsRuleTemplateParam rewardPercentValue2TemplateParam(BigDecimal rewardPercentValue) {
        return DroolsTemplateRuleUtils.toTemplateParam(rewardPercentValue.setScale(4, RoundingMode.HALF_DOWN).divide(ONEHUNDRED, RoundingMode.HALF_DOWN));
    }

    public static String applyPercentageReward(DroolsRuleTemplateParam rewardPercentValueTemplateParam) {
        return "$trx.getEffectiveAmount().multiply(%s).setScale(2, java.math.RoundingMode.HALF_DOWN)".formatted(rewardPercentValueTemplateParam.getParam());
    }
}
