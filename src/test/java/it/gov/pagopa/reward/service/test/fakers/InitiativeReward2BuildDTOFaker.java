package it.gov.pagopa.reward.service.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.FieldEnumRewardDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardLimitDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.service.utils.TestUtils;

import java.util.ArrayList;
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

        out.setRewardRule(new ArrayList<>());
        out.getRewardRule().add(new RewardLimitDTO(FieldEnumRewardDTO.REWARD_LIMIT,"daily","700.00"));
        out.getRewardRule().add(new RewardValueDTO(FieldEnumRewardDTO.REWARD_VALUE,200));


        TestUtils.checkNotNullFields(out);
        return out;
    }

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(new Random(bias)));
    }
}
