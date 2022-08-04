package it.gov.pagopa.reward.test.fakers;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.build.InitiativeGeneralDTO;
import it.gov.pagopa.reward.dto.build.InitiativeReward2BuildDTO;
import it.gov.pagopa.reward.dto.rule.reward.InitiativeRewardRule;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import it.gov.pagopa.reward.dto.rule.reward.RewardValueDTO;
import it.gov.pagopa.reward.dto.rule.trx.*;
import it.gov.pagopa.reward.test.fakers.rule.*;
import it.gov.pagopa.reward.test.utils.TestUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class InitiativeReward2BuildDTOFaker {
    private InitiativeReward2BuildDTOFaker(){}

    private static final Random randomGenerator = new Random();

    private static Random getRandom(Integer bias) {
        return bias == null ? randomGenerator : new Random(bias);
    }

    private static int getRandomPositiveNumber(Integer bias) {
        return Math.abs(getRandom(bias).nextInt());
    }

    private static final FakeValuesService fakeValuesServiceGlobal = new FakeValuesService(new Locale("it"), new RandomService(null));

    private static FakeValuesService getFakeValuesService(Integer bias) {
        return bias == null ? fakeValuesServiceGlobal : new FakeValuesService(new Locale("it"), new RandomService(getRandom(bias)));
    }

    /** It will return an example of {@link InitiativeReward2BuildDTO}. Providing a bias, it will return a pseudo-casual object */
    public static InitiativeReward2BuildDTO mockInstance(Integer bias){
        return mockInstance(bias, null, null);
    }
    /** It will return an example of {@link InitiativeReward2BuildDTO}. Providing a bias, it will return a pseudo-casual object */
    public static InitiativeReward2BuildDTO mockInstance(Integer bias, Set<Class<? extends InitiativeTrxCondition>> conditions2Configure, Class<? extends InitiativeRewardRule> reward2configure){
        return mockInstanceBuilder(bias, conditions2Configure, reward2configure).build();
    }

    /** It will return an example of builder to obtain a {@link InitiativeReward2BuildDTO}. Providing a bias, it will return a pseudo-casual object */
    public static InitiativeReward2BuildDTO.InitiativeReward2BuildDTOBuilder<?,?> mockInstanceBuilder(Integer bias, Set<Class<? extends InitiativeTrxCondition>> conditions2Configure, Class<? extends InitiativeRewardRule> reward2configure){
        InitiativeReward2BuildDTO.InitiativeReward2BuildDTOBuilder<?,?> out = InitiativeReward2BuildDTO.builder();

        bias = ObjectUtils.firstNonNull(bias, getRandomPositiveNumber(null));

        FakeValuesService fakeValuesService = getFakeValuesService(bias);

        out.initiativeId("ID_%d_%s".formatted(bias, fakeValuesService.bothify("???")));
        out.initiativeName("NAME_%d_%s".formatted(bias, fakeValuesService.bothify("???")));

        InitiativeGeneralDTO initiativeGeneral = new InitiativeGeneralDTO();
        out.general(initiativeGeneral);

        InitiativeTrxConditions trxRule = new InitiativeTrxConditions();
        out.trxRule(trxRule);

        if(conditions2Configure==null || conditions2Configure.contains(DayOfWeekDTO.class)){
            trxRule.setDaysOfWeek(DayOfWeekDTOFaker.mockInstance(bias));
        }

        if(conditions2Configure==null || conditions2Configure.contains(MccFilterDTO.class)){
            trxRule.setMccFilter(MccFilterDTOFaker.mockInstance(bias));
        }


        if(conditions2Configure==null || conditions2Configure.contains(RewardLimitsDTO.class)) {
            trxRule.setRewardLimits(List.of(
                    new RewardLimitsDTO(RewardLimitsDTO.RewardLimitFrequency.DAILY, BigDecimal.valueOf((bias + 1) * 10L)),
                    new RewardLimitsDTO(RewardLimitsDTO.RewardLimitFrequency.WEEKLY, BigDecimal.valueOf((bias + 1) * 70L)),
                    new RewardLimitsDTO(RewardLimitsDTO.RewardLimitFrequency.MONTHLY, BigDecimal.valueOf((bias + 1) * 300L)),
                    new RewardLimitsDTO(RewardLimitsDTO.RewardLimitFrequency.YEARLY, BigDecimal.valueOf((bias + 1) * 3650L))
            ));
        }

        if(conditions2Configure==null || conditions2Configure.contains(ThresholdDTO.class)){
            trxRule.setThreshold(ThresholdDTOFaker.mockInstance(bias));
        }

        if(conditions2Configure==null || conditions2Configure.contains(TrxCountDTO.class)){
            trxRule.setTrxCount(TrxCountDTOFaker.mockInstance(bias));
        }


        out.rewardRule(
                bias%5==3 || (reward2configure == RewardGroupsDTO.class)
                ? RewardGroupsDTOFaker.mockInstance(bias)
                : new RewardValueDTO(BigDecimal.valueOf((Math.abs(bias)+1)*0.23).setScale(2, RoundingMode.HALF_DOWN)));

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

}
