package it.gov.pagopa.reward.test.fakers.rule;

import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;
import it.gov.pagopa.reward.dto.rule.trx.TrxCountDTO;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Locale;
import java.util.Random;

public class TrxCountDTOFaker {
    private TrxCountDTOFaker(){}

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

    /** It will return an example of {@link TrxCountDTO}. Providing a bias, it will return a pseudo-casual object */
    public static TrxCountDTO mockInstance(Integer bias){
        bias = ObjectUtils.firstNonNull(bias, getRandomPositiveNumber(null));
        return TrxCountDTO.builder()
                .from(bias.longValue())
                .fromIncluded(bias % 2 == 0)
                .to(bias+10L)
                .toIncluded(bias % 3 == 0)
                .build();
    }
}
