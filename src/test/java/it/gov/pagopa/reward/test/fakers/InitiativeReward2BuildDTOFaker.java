package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.InitiativeTrxConditions;
import it.gov.pagopa.reward.dto.rule.trx.RewardLimitsDTO;
import it.gov.pagopa.reward.test.utils.TestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class InitiativeReward2BuildDTOFaker {
    private InitiativeReward2BuildDTOFaker(){}

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService());

    /** It will return an example of {@link InitiativeReward2BuildDTO}. Providing a bias, it will return a pseudo-casual object */
    public static InitiativeReward2BuildDTO mockInstance(Integer bias){
        InitiativeReward2BuildDTO out = new InitiativeReward2BuildDTO();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.setInitiativeId(fakeValuesService.bothify("?????"));
        out.setInitiativeName(fakeValuesService.bothify("?????"));

        out.setTrxRule(new InitiativeTrxConditions());
        out.getTrxRule().setRewardLimits(List.of(new RewardLimitsDTO(RewardLimitsDTO.RewardLimitFrequency.DAILY,BigDecimal.valueOf(700.00))));
        out.setRewardRule(new RewardValueDTO(BigDecimal.valueOf(bias*0.23)));


        TestUtils.checkNotNullFields(out);
        return out;
    }

    public static InitiativeReward2BuildDTO mockInstanceWithFrequencyType(Integer bias, RewardLimitsDTO.RewardLimitFrequency frequency){
        InitiativeReward2BuildDTO out = new InitiativeReward2BuildDTO();

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.setInitiativeId(fakeValuesService.bothify("?????"));
        out.setInitiativeName(fakeValuesService.bothify("?????"));

        out.setTrxRule(new InitiativeTrxConditions());
        out.getTrxRule().setRewardLimits(List.of(new RewardLimitsDTO(frequency,BigDecimal.valueOf(700.00))));
        out.setRewardRule(new RewardValueDTO(BigDecimal.valueOf(bias*0.23)));


        TestUtils.checkNotNullFields(out);
        return out;
    }

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(new Random(bias)));
    }
}
