package it.gov.pagopa.reward.test.fakers.rule;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.rule.reward.RewardGroupsDTO;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RewardGroupsDTOFaker {
    private RewardGroupsDTOFaker(){}

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

    /** It will return an example of {@link RewardGroupsDTO}. Providing a bias, it will return a pseudo-casual object */
    public static RewardGroupsDTO mockInstance(Integer bias){
        bias = ObjectUtils.firstNonNull(bias, getRandomPositiveNumber(null));
        return RewardGroupsDTO.builder().rewardGroups(
                IntStream.rangeClosed(0, bias%3)
                        .mapToObj(i-> RewardGroupsDTO.RewardGroupDTO.builder()
                                .from(BigDecimal.valueOf(i*10L))
                                .to(BigDecimal.valueOf(i*10L+5))
                                .rewardValue(BigDecimal.valueOf(((i+1) * 10L)%100))
                                .build())
                        .collect(Collectors.toList())
        ).build();
    }
}
