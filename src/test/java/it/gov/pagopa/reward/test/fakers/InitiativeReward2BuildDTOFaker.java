package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTOFaker;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.*;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class InitiativeReward2BuildDTOFaker {
    private InitiativeReward2BuildDTOFaker(){}

    private static final Random randomGenerator = new Random();

    private static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService(null));

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(getRandom(bias)));
    }

    /** It will return an example of {@link InitiativeReward2BuildDTO}. Providing a bias, it will return a pseudo-casual object */
    public static InitiativeReward2BuildDTO mockInstance(Integer bias){
        InitiativeReward2BuildDTO out = new InitiativeReward2BuildDTO();

        bias = ObjectUtils.firstNonNull(bias, getRandom(null).nextInt());

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.setInitiativeId("ID_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.setInitiativeName("NAME_%d_%s".formatted(bias, fakeValuesService.bothify("???")));

        out.setTrxRule(new InitiativeTrxConditions());

        out.getTrxRule().setDaysOfWeek(DayOfWeekDTOFaker.mockInstance(bias));

        out.getTrxRule().setMccFilter(MccFilterDTOFaker.mockInstance(bias));

        out.getTrxRule().setRewardLimits(List.of(
                new RewardLimitsDTO(RewardLimitsDTO.RewardLimitFrequency.DAILY,BigDecimal.valueOf((bias+1)* 10L)),
                new RewardLimitsDTO(RewardLimitsDTO.RewardLimitFrequency.WEEKLY,BigDecimal.valueOf((bias+1)* 70L)),
                new RewardLimitsDTO(RewardLimitsDTO.RewardLimitFrequency.MONTHLY,BigDecimal.valueOf((bias+1)* 300L)),
                new RewardLimitsDTO(RewardLimitsDTO.RewardLimitFrequency.YEARLY,BigDecimal.valueOf((bias+1)* 3650L))
        ));

        out.getTrxRule().setThreshold(ThresholdDTOFaker.mockInstance(bias));

        out.getTrxRule().setTrxCount(TrxCountDTOFaker.mockInstance(bias));

        out.setRewardRule(
                bias%5==0
                ? RewardGroupsDTOFaker.mockInstance(bias)
                : new RewardValueDTO(BigDecimal.valueOf(bias*0.23)));

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
